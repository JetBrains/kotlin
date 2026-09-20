/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

/**
 * Runs a real test task of this repository and checks what TeamCity is told about its tests, both
 * when the task runs and when it is served from the build cache or skipped as up to date.
 *
 * The subject is `:repo:test-inventory-fixture:test`, a module whose tests exist only to be recorded
 * and replayed, and which is small enough to assert both in full. It is driven as a whole Gradle
 * build rather than through a synthetic project, because everything under test hangs off the
 * repository's own build logic: [TestExecutionsListener] is attached by `project-tests-convention`,
 * the file it writes is a declared task output, and [TestBuildCacheTeamCityCompatibilityService] is
 * registered only for a TeamCity build.
 *
 * Every property that decides what is recorded is passed explicitly rather than left to the
 * environment, so that these tests mean the same thing when they themselves run on TeamCity:
 * `-Pteamcity`, which `TEAMCITY_VERSION` would otherwise decide; the test federation, which would
 * otherwise deselect tests according to the mode the surrounding build runs in; and the retry count,
 * which decides how many times a failing test is recorded.
 */
class TestBuildCacheTeamCityCompatibilityFunctionalTest {

    /**
     * Empties the build cache the tests share, so that each of them starts from nothing in it.
     *
     * One directory for the whole class, rather than one per test: its path is a Gradle property, and
     * a new one per test invalidates the nested builds' configuration cache, making every test pay
     * for configuring the whole repository again.
     */
    @BeforeEach
    fun emptyTheBuildCache() {
        cache.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * The recording a run produces, and the shape the other tests replay from.
     *
     * A task that actually ran reports nothing itself: TeamCity's own Gradle runner already reports
     * those tests, and replaying them as well would report every test twice.
     */
    @Test
    fun `a task that runs records its tests and reports nothing`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH)

        assertEquals(TaskOutcome.SUCCESS, result.fixtureTaskOutcome)
        assertEquals(expectedRecording, recordedExecutions().withoutDurations())
        assertEquals(emptyList(), result.replayedTestMessages)
    }

    /**
     * The point of the whole feature: a task nobody ran still reports its tests.
     *
     * The recording itself is restored by the cache along with the task's other outputs, which is
     * checked here too - without it there would be nothing to replay.
     */
    @Test
    fun `a task served from the build cache replays its recorded tests`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH)
        val recorded = recordedExecutions()

        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        assertFalse(executionsFile.exists(), "'cleanTest' should have removed ${executionsFile.name}")

        val result = runTasks(cache, FIXTURE_TASK_PATH)

        assertEquals(TaskOutcome.FROM_CACHE, result.fixtureTaskOutcome)
        assertEquals(recorded, recordedExecutions(), "The build cache should restore the recording unchanged")
        assertContains(result.quotableOutput, CONFIGURATION_CACHE_REUSED)
        assertEquals(expectedReplay, result.replayedTestMessages)
    }

    /**
     * An up-to-date task is as silent as a cached one, and equally in need of replaying: TeamCity is
     * told about a test by the build that ran it, and a build that skipped it reports nothing at all.
     *
     * Both invocations are identical, so the second one also reuses the configuration cache - the
     * shape CI runs in, and the one where the service has to come back from a serialized graph rather
     * than from a freshly configured build.
     */
    @Test
    fun `an up-to-date task replays its recorded tests`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH)

        val result = runTasks(cache, FIXTURE_TASK_PATH)

        assertEquals(TaskOutcome.UP_TO_DATE, result.fixtureTaskOutcome)
        assertContains(result.quotableOutput, CONFIGURATION_CACHE_REUSED)
        assertEquals(expectedReplay, result.replayedTestMessages)
    }

    /** The same, with the configuration cache out of the picture. */
    @Test
    fun `a task served from the build cache replays its recorded tests without the configuration cache`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH, configurationCache = false)

        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH, configurationCache = false)

        assertEquals(TaskOutcome.FROM_CACHE, result.fixtureTaskOutcome)
        assertFalse(
            "Configuration cache entry" in result.quotableOutput,
            "This test is only worth anything while the configuration cache stays out of the build",
        )
        assertEquals(expectedReplay, result.replayedTestMessages)
    }

    /**
     * One service replays the whole build, so a build with two test tasks in it has to replay both,
     * each under a flow of its own.
     *
     * Nothing else here would notice if it stopped: every other test runs a build whose only test
     * task is the fixture's, and a map that kept a single task, or a flow id fixed for the build
     * rather than taken per task, would leave all of them green.
     */
    @Test
    fun `the test tasks of one build are each replayed under their own flow`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH, SECOND_TASK_CLEAN_PATH)
        runTasks(cache, FIXTURE_TASK_PATH, SECOND_TASK_PATH)

        cleanTasks(FIXTURE_CLEAN_TASK_PATH, SECOND_TASK_CLEAN_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH, SECOND_TASK_PATH)

        assertEquals(TaskOutcome.FROM_CACHE, result.fixtureTaskOutcome)
        assertEquals(TaskOutcome.FROM_CACHE, result.outcomeOf(SECOND_TASK_PATH))
        assertContains(result.quotableOutput, CONFIGURATION_CACHE_REUSED)

        val replayed = result.replayedTestMessages
        assertEquals(expectedReplay, replayed.inFlow(REPLAY_FLOW_ID))
        assertEquals(expectedSecondTaskReplay, replayed.inFlow(SECOND_TASK_FLOW_ID))
        assertEquals(
            expectedReplay.size + expectedSecondTaskReplay.size,
            replayed.size,
            "Every replayed message should carry the flow of the task it was recorded for",
        )
    }

    /**
     * Outside a TeamCity build there is nobody to report to, and the service messages would only be
     * noise in a local build's console.
     */
    @Test
    fun `a task served from the build cache replays nothing outside a TeamCity build`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH, teamCity = false)

        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH, teamCity = false)

        assertEquals(TaskOutcome.FROM_CACHE, result.fixtureTaskOutcome)
        assertEquals(emptyList(), result.replayedTestMessages)
    }

    /**
     * A failed test in a task that nevertheless succeeded, which is what a tolerated failure or a
     * retried flaky test leaves behind, and what makes a replayed `testFailed` possible at all.
     *
     * The failure itself is gone by then - the recording keeps a status, not a stack trace - so the
     * message says where the verdict came from instead of pretending to explain it.
     */
    @Test
    fun `a failed test is replayed as a failure`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH, failing = true)

        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH, failing = true)

        assertEquals(TaskOutcome.FROM_CACHE, result.fixtureTaskOutcome)
        assertEquals(
            replayedFailedTest,
            result.replayedTestMessages.about("failing()"),
        )
    }

    /**
     * Why the order of the recorded tests is load-bearing: a retried test is recorded once per
     * attempt, and replaying the attempts as they happened is what lets TeamCity apply its own retry
     * handling. Merging them by name, or keeping only the last, would report something that did not
     * happen.
     */
    @Test
    fun `a retried test is replayed once per attempt`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH, failing = true, maxRetries = 3)

        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH, failing = true, maxRetries = 3)

        assertEquals(TaskOutcome.FROM_CACHE, result.fixtureTaskOutcome)
        assertEquals(
            List(4) { replayedFailedTest }.flatten(),
            result.replayedTestMessages.about("failing()"),
            "The first attempt and its three retries should each be replayed",
        )
    }

    companion object {
        /** The only build cache the nested builds can reach, see [emptyTheBuildCache]. */
        @JvmStatic
        @TempDir
        lateinit var cache: File
    }
}

private const val FIXTURE_TASK_PATH = ":repo:test-inventory-fixture:test"
private const val FIXTURE_CLEAN_TASK_PATH = ":repo:test-inventory-fixture:cleanTest"
private const val FIXTURE_CLASS = "org.jetbrains.kotlin.testInventory.TestInventoryFixtureTest"
private const val FIXTURE_NESTED_CLASS = $$"$$FIXTURE_CLASS$NestedTests"
private const val FIXTURE_IGNORED_TEST = "$FIXTURE_CLASS.ignored()"
private const val FIXTURE_FAILING_TEST = "$FIXTURE_CLASS.failing()"

private const val REPLAY_FLOW_ID = "TestReplay$FIXTURE_TASK_PATH"

/** The fixture's second test task, whose one test is all it runs, and the flow of its replay. */
private const val SECOND_TASK_PATH = ":repo:test-inventory-fixture:secondTest"
private const val SECOND_TASK_CLEAN_PATH = ":repo:test-inventory-fixture:cleanSecondTest"
private const val SECOND_TASK_FLOW_ID = "TestReplay$SECOND_TASK_PATH"
private const val SECOND_TASK_TEST =
    "org.jetbrains.kotlin.testInventory.SecondTestInventoryFixtureTest.recordedBySecondTestTask()"

/**
 * What makes a line a service message, and what these tests take back off every line they keep.
 *
 * Not cosmetic: whatever a failing assertion prints lands in the console of the build that runs these
 * tests, and a line still carrying the marker would be read by the TeamCity running *them* as one of
 * its own tests starting, or failing. The filter that looks for the marker is what asserts it is there.
 */
private const val SERVICE_MESSAGE_MARKER = "##teamcity"

/**
 * What a build says when it did not configure itself but read the whole task graph back.
 *
 * Asserted wherever a replay is: it is the shape CI runs in, and the one where the service doing the
 * replaying, and its registration as a build listener, come back from a serialized graph rather than
 * from `configureTestInventory` having just run.
 */
private const val CONFIGURATION_CACHE_REUSED = "Configuration cache entry reused."

/** The text a replayed failure carries in place of the failure details the recording does not keep. */
private const val FAILURE_DETAILS_UNAVAILABLE =
    "Test failed when this task last ran; replayed from cached test results, no failure details recorded"

/**
 * The whole of what the fixture's run is recorded as, which is also the format's documentation by
 * example - see the `### Format` section of [TestExecutionsListener].
 *
 * Worth reading for the three states of `className`: absent where the enclosing suite is the test's
 * class, present on the parameterized invocations, whose own suite is the container rather than the
 * class, and absent again on the nested class's test, whose suite is that nested class.
 */
private val expectedRecording = """
    {
      "formatVersion": 1,
      "taskPath": "$FIXTURE_TASK_PATH",
      "duration": <ms>,
      "suites": [
        {
          "name": "$FIXTURE_CLASS",
          "duration": <ms>,
          "suites": [
            {
              "name": "parameterized(int)",
              "duration": <ms>,
              "suites": [],
              "tests": [
                { "name": "parameterized([1] 1)", "className": "$FIXTURE_CLASS", "status": "OK", "duration": <ms> },
                { "name": "parameterized([2] 2)", "className": "$FIXTURE_CLASS", "status": "OK", "duration": <ms> }
              ]
            },
            {
              "name": "$FIXTURE_NESTED_CLASS",
              "duration": <ms>,
              "suites": [],
              "tests": [
                { "name": "nested()", "status": "OK", "duration": <ms> }
              ]
            }
          ],
          "tests": [
            { "name": "failing()", "status": "OK", "duration": <ms> },
            { "name": "passing()", "status": "OK", "duration": <ms> },
            { "name": "ignored()", "status": "Ignored", "duration": <ms> }
          ]
        }
      ],
      "tests": []
    }

""".trimIndent()

/** The replay of the fixture's one ignored test, which TeamCity is told about before it is finished. */
private val replayedIgnoredTest = listOf(
    "[testStarted name='$FIXTURE_IGNORED_TEST' flowId='$REPLAY_FLOW_ID']",
    "[testIgnored name='$FIXTURE_IGNORED_TEST' flowId='$REPLAY_FLOW_ID']",
    "[testFinished name='$FIXTURE_IGNORED_TEST' duration='<ms>' flowId='$REPLAY_FLOW_ID']",
)

/** The replay of one attempt at the fixture's one failing test. */
private val replayedFailedTest = listOf(
    "[testStarted name='$FIXTURE_FAILING_TEST' flowId='$REPLAY_FLOW_ID']",
    "[testFailed name='$FIXTURE_FAILING_TEST' message='$FAILURE_DETAILS_UNAVAILABLE' flowId='$REPLAY_FLOW_ID']",
    "[testFinished name='$FIXTURE_FAILING_TEST' duration='<ms>' flowId='$REPLAY_FLOW_ID']",
)

private fun replayedTest(name: String, flowId: String = REPLAY_FLOW_ID) = listOf(
    "[testStarted name='$name' flowId='$flowId']",
    "[testFinished name='$name' duration='<ms>' flowId='$flowId']",
)

/** What a replay of the fixture's second test task has to say - its one test, in its own flow. */
private val expectedSecondTaskReplay = replayedTest(SECOND_TASK_TEST, SECOND_TASK_FLOW_ID)

private fun replayedSuite(name: String, vararg contents: List<String>): List<String> = buildList {
    add("[testSuiteStarted name='$name' flowId='$REPLAY_FLOW_ID']")
    contents.forEach(::addAll)
    add("[testSuiteFinished name='$name' flowId='$REPLAY_FLOW_ID']")
}

/** The messages of [groups], one after another, so that a nesting can be written as one. */
private fun replayedMessages(vararg groups: List<String>): List<String> = groups.flatMap { it }

/**
 * What a replay of [expectedRecording] has to say.
 *
 * The suites are not the recorded ones: a class is spelled as part of a test's name and never as a
 * suite of its own, so the class suite is gone for its own three tests, which are reported at the top
 * level, and kept for everything nested inside it. The parameterized container stands for neither a
 * class nor nothing, so it survives, with the class still in the names of its invocations.
 * The `|[` and `|]` are how a service message spells a bracket.
 */
private val expectedReplay = replayedMessages(
    replayedTest("$FIXTURE_CLASS.failing()"),
    replayedTest("$FIXTURE_CLASS.passing()"),
    replayedIgnoredTest,
    replayedSuite(
        FIXTURE_CLASS,
        replayedTest("$FIXTURE_NESTED_CLASS.nested()"),
        replayedSuite(
            "parameterized(int)",
            replayedTest("$FIXTURE_CLASS.parameterized(|[1|] 1)"),
            replayedTest("$FIXTURE_CLASS.parameterized(|[2|] 2)"),
        ),
    ),
)

/** The test task's working directory is the root of the repository, see this module's build script. */
private val repoRoot: File get() = Path("").toAbsolutePath().toFile()

private val executionsFile: File
    get() = repoRoot.resolve("repo/test-inventory-fixture/build/test-inventory/test/test-executions.json")

private fun recordedExecutions(): String = executionsFile.readText()

/**
 * Replaces every duration with a placeholder: they are real timings and differ from run to run, while
 * everything around them is what these tests are about.
 */
private fun String.withoutDurations(): String = replace(Regex("\"duration\": \\d+"), "\"duration\": <ms>")

private fun BuildResult.outcomeOf(taskPath: String): TaskOutcome =
    (task(taskPath) ?: fail("No '$taskPath' in the build result")).outcome

private val BuildResult.fixtureTaskOutcome: TaskOutcome get() = outcomeOf(FIXTURE_TASK_PATH)

/**
 * The test related service messages of a build, without their [SERVICE_MESSAGE_MARKER] and with the
 * durations replaced as in [withoutDurations].
 *
 * Every `test...` message is taken, not only the ones carrying the replay's flow id, so that a message
 * sent under the wrong flow id shows up as a difference rather than as silence.
 */
private val BuildResult.replayedTestMessages: List<String>
    get() = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("$SERVICE_MESSAGE_MARKER[test") }
        .map { it.removePrefix(SERVICE_MESSAGE_MARKER) }
        .map { it.replace(Regex("duration='\\d+'"), "duration='<ms>'") }
        .toList()

/** A build's console output, with every service message marker taken off, see [SERVICE_MESSAGE_MARKER]. */
private val BuildResult.quotableOutput: String get() = output.replace(SERVICE_MESSAGE_MARKER, "")

/** The messages naming one test, for a test interested in that one rather than in the whole replay. */
private fun List<String>.about(testName: String): List<String> = filter { testName in it }

/** The messages of one task's replay, for a build that replayed more than one test task. */
private fun List<String>.inFlow(flowId: String): List<String> = filter { "flowId='$flowId'" in it }

/**
 * Runs [tasks], with [cache] as the only build cache the build can reach - an empty one at the start
 * of every test, and the repository's remote cache explicitly out of reach, so that a run either
 * executes a task or takes it from what an earlier run of the same test stored.
 */
private fun runTasks(
    cache: File,
    vararg tasks: String,
    teamCity: Boolean = true,
    configurationCache: Boolean = true,
    failing: Boolean = false,
    maxRetries: Int = 0,
): BuildResult = runBuild(
    buildList {
        addAll(tasks)
        add("-Pteamcity=$teamCity")
        // Which tests of the fixture run has to depend on nothing but the fixture itself.
        add("-Ptest.federation.enabled=false")
        add("-PtestInventoryFixture.failing=$failing")
        add("-Pkotlin.build.testRetry.maxRetries=$maxRetries")
        add("--build-cache")
        add("-Pkotlin.build.cache.local.enabled=true")
        add("-Pkotlin.build.cache.local.directory=${cache.absolutePath}")
        // An empty url is how this repository's settings spell "no remote build cache".
        add("-Pkotlin.build.cache.url=")
        add(if (configurationCache) "--configuration-cache" else "--no-configuration-cache")
    }
)

/**
 * Runs the `clean...` [tasks] given, deleting the outputs of the tasks they stand for, so that the
 * next run of those either executes them or takes them from the cache.
 */
private fun cleanTasks(vararg tasks: String) {
    runBuild(tasks.toList())
}

private fun runBuild(arguments: List<String>): BuildResult {
    val gradleUserHome = System.getenv("GRADLE_USER_HOME") ?: error("Missing 'GRADLE_USER_HOME' environment variable")
    val runner = GradleRunner.create()
        .withProjectDir(repoRoot)
        .withTestKitDir(File(gradleUserHome))
        .withArguments(arguments + "-Dorg.gradle.daemon.idletimeout=${5.seconds.inWholeMilliseconds}")

    return try {
        runner.build()
    } catch (failure: UnexpectedBuildFailure) {
        error(
            buildString {
                appendLine("Build failed: ${arguments.joinToString(" ")}")
                appendLine("Output:")
                failure.buildResult.quotableOutput.lineSequence().forEach { appendLine(it) }
            }
        )
    }
}
