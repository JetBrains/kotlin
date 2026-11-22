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
 * Runs a real test task of this repository and checks what TeamCity is told about its tests, when
 * the task runs and when it is served from the build cache or skipped as up to date.
 *
 * The subject is `:repo:test-inventory-fixture`, a module whose tests exist to be recorded and
 * replayed and is small enough to assert in full. It is driven as a whole build because everything
 * under test hangs off the repository's own build logic.
 *
 * Every property that decides what is recorded is passed explicitly - `-Pteamcity`, the test
 * federation, the retry count - so that these tests mean the same when they run on TeamCity.
 */
class TestBuildCacheTeamCityCompatibilityFunctionalTest {

    /**
     * Empties the build cache the tests share, so that each starts from nothing in it.
     *
     * One directory for the whole class: its path is a Gradle property, and a new one per test would
     * invalidate the nested builds' configuration cache.
     */
    @BeforeEach
    fun emptyTheBuildCache() {
        cache.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * The recording a run produces, and the shape the other tests replay from. A task that ran
     * reports nothing itself: TeamCity's own runner already did, and replaying would double it.
     */
    @Test
    fun `a task that runs records its tests and reports nothing`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        val result = runTasks(cache, FIXTURE_TASK_PATH)

        assertEquals(TaskOutcome.SUCCESS, result.fixtureTaskOutcome)
        assertEquals(expectedRecording, recordedExecutions().withoutVolatileParts())
        assertEquals(emptyList(), result.replayedTestMessages)
    }

    /**
     * The point of the whole feature: a task nobody ran still reports its tests. The recording comes
     * back from the cache with the task's other outputs, without which there would be nothing to replay.
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
        assertEquals(1, result.replayedTestMessages.flows.size)
        assertEquals(expectedReplay, result.replayedTestMessages.withoutFlowIds())
    }

    /**
     * An up-to-date task is as silent as a cached one, and equally in need of replaying.
     *
     * Both invocations are identical, so the second also reuses the configuration cache - the shape
     * CI runs in, where the service comes back from a serialized graph rather than a configured build.
     */
    @Test
    fun `an up-to-date task replays its recorded tests`() {
        cleanTasks(FIXTURE_CLEAN_TASK_PATH)
        runTasks(cache, FIXTURE_TASK_PATH)

        val result = runTasks(cache, FIXTURE_TASK_PATH)

        assertEquals(TaskOutcome.UP_TO_DATE, result.fixtureTaskOutcome)
        assertContains(result.quotableOutput, CONFIGURATION_CACHE_REUSED)
        assertEquals(expectedReplay, result.replayedTestMessages.withoutFlowIds())
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
        assertEquals(expectedReplay, result.replayedTestMessages.withoutFlowIds())
    }

    /**
     * One service replays the whole build, so a build with two test tasks has to replay both, each
     * under a flow of its own. Every other test here runs a build with one test task in it, so a map
     * that kept a single task, or a flow id fixed per build, would leave them all green.
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
        // Which numbered flow a task got is nothing a test can know, so each is found by a test of
        // its own: a task's tests all share the flow its replay was sent under.
        assertEquals(2, replayed.flows.size, "The two test tasks should replay under a flow each: $replayed")
        assertEquals(expectedReplay, replayed.inFlow(replayed.flowOf(FIXTURE_IGNORED_TEST)).withoutFlowIds())
        assertEquals(expectedSecondTaskReplay, replayed.inFlow(replayed.flowOf(SECOND_TASK_TEST)).withoutFlowIds())
        assertEquals(
            expectedReplay.size + expectedSecondTaskReplay.size,
            replayed.size,
            "Every replayed message should be in one of the two flows",
        )
    }

    /** Outside a TeamCity build there is nobody to report to, and the messages would only be noise. */
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
     * A failed test in a task that nevertheless succeeded - what a tolerated failure or a retried
     * flaky test leaves behind, and what makes a replayed `testFailed` possible at all. The failure
     * itself is gone by then, so the message says where the verdict came from.
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
            result.replayedTestMessages.about("failing()").withoutFlowIds(),
        )
    }

    /**
     * Why the recorded order is load bearing: a retried test is recorded once per attempt, and
     * replaying them as they happened is what lets TeamCity apply its own retry handling.
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
            result.replayedTestMessages.about("failing()").withoutFlowIds(),
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

/** The fixture's second test task, whose one test is all it runs. */
private const val SECOND_TASK_PATH = ":repo:test-inventory-fixture:secondTest"
private const val SECOND_TASK_CLEAN_PATH = ":repo:test-inventory-fixture:cleanSecondTest"
private const val SECOND_TASK_TEST =
    "org.jetbrains.kotlin.testInventory.SecondTestInventoryFixtureTest.recordedBySecondTestTask()"

/**
 * What makes a line a service message, taken back off every line these tests keep: a line still
 * carrying it would be read by the TeamCity running *them* as one of its own tests, once a failing
 * assertion prints it. The filter that looks for the marker is what asserts it is there.
 */
private const val SERVICE_MESSAGE_MARKER = "##teamcity"

/**
 * What a build says when it read the task graph back instead of configuring itself. Asserted
 * wherever a replay is: it is the shape CI runs in, and the one where the service and its
 * subscription come back from a serialized graph rather than from `configureTestInventory`.
 */
private const val CONFIGURATION_CACHE_REUSED = "Configuration cache entry reused."

/** The text a replayed failure carries in place of the failure details the recording does not keep. */
private const val FAILURE_DETAILS_UNAVAILABLE =
    "Test failed when this task last ran; replayed from cached test results, no failure details recorded"

/**
 * The whole of what the fixture's run is recorded as, one record per line in the order Gradle
 * reported the events, the format being documented in the `### Format` section of
 * [TestExecutionsListener]. Worth reading for what a `className` is left to the parent suite to
 * say, and for the suites Gradle inserts itself, which a recording keeps and a replay drops.
 *
 * The times and the executor's number are masked in the comparison, see [withoutVolatileParts].
 */
private val expectedRecording = """
    {"type":"header","formatVersion":1,"taskPath":"$FIXTURE_TASK_PATH"}
    {"type":"suite","id":1,"name":"Gradle Test Run $FIXTURE_TASK_PATH"}
    {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor <n>"}
    {"type":"suite","id":3,"parent":2,"name":"$FIXTURE_CLASS","displayName":"TestInventoryFixtureTest","className":"$FIXTURE_CLASS","source":{"kind":"class"}}
    {"type":"test","parent":3,"name":"failing()","source":{"kind":"method","methodName":"failing"},"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"test","parent":3,"name":"passing()","source":{"kind":"method","methodName":"passing"},"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"suite","id":4,"parent":3,"name":"parameterized(int)"}
    {"type":"test","parent":4,"name":"parameterized(int)[1]","displayName":"[1] 1","source":{"kind":"method","methodName":"parameterized"},"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"test","parent":4,"name":"parameterized(int)[2]","displayName":"[2] 2","source":{"kind":"method","methodName":"parameterized"},"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"suiteEnd","id":4,"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"test","parent":3,"name":"ignored()","source":{"kind":"method","methodName":"ignored"},"resultType":"SKIPPED","startTime":<ms>,"endTime":<ms>}
    {"type":"suite","id":5,"parent":3,"name":"$FIXTURE_NESTED_CLASS","displayName":"NestedTests","className":"$FIXTURE_NESTED_CLASS","source":{"kind":"class"}}
    {"type":"test","parent":5,"name":"nested()","source":{"kind":"method","methodName":"nested"},"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"suiteEnd","id":5,"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"suiteEnd","id":2,"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}
    {"type":"suiteEnd","id":1,"resultType":"SUCCESS","startTime":<ms>,"endTime":<ms>}

""".trimIndent()

/*
 * The messages the expectations are built of carry no flow id: the service numbers a flow per
 * replayed task, and which number a task got is nothing a test can know. Where the flows are still
 * asserted is `flows` and `flowOf` below.
 */
/** The replay of the fixture's one ignored test, which TeamCity is told about before it is finished. */
private val replayedIgnoredTest = listOf(
    "[testStarted name='$FIXTURE_IGNORED_TEST']",
    "[testIgnored name='$FIXTURE_IGNORED_TEST']",
    "[testFinished name='$FIXTURE_IGNORED_TEST' duration='<ms>']",
)

/** The replay of one attempt at the fixture's one failing test. */
private val replayedFailedTest = listOf(
    "[testStarted name='$FIXTURE_FAILING_TEST']",
    "[testFailed name='$FIXTURE_FAILING_TEST' message='$FAILURE_DETAILS_UNAVAILABLE']",
    "[testFinished name='$FIXTURE_FAILING_TEST' duration='<ms>']",
)

private fun replayedTest(name: String) = listOf(
    "[testStarted name='$name']",
    "[testFinished name='$name' duration='<ms>']",
)

/** What a replay of the fixture's second test task has to say - its one test, in a flow of its own. */
private val expectedSecondTaskReplay = replayedTest(SECOND_TASK_TEST)

private fun replayedSuite(name: String, vararg contents: List<String>): List<String> = buildList {
    add("[testSuiteStarted name='$name']")
    contents.forEach(::addAll)
    add("[testSuiteFinished name='$name']")
}

/** The messages of [groups], one after another, so that a nesting can be written as one. */
private fun replayedMessages(vararg groups: List<String>): List<String> = groups.flatMap { it }

/**
 * What a replay of [expectedRecording] has to say.
 *
 * The suites are not the recorded ones: a class is part of a test's name rather than a suite, so the
 * class suite is gone for its own three tests and kept for what is nested in it. `|[` and `|]` are
 * how a service message spells a bracket.
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
    get() = repoRoot.resolve("repo/test-inventory-fixture/build/test-inventory/test/test-executions.jsonl")

private fun recordedExecutions(): String = executionsFile.readText()

/**
 * Replaces what two recordings of the same tests cannot help differing in: the times, which are
 * real timings, and the executor's number, which depends on how Gradle shares the forks out.
 */
private fun String.withoutVolatileParts(): String = replace(Regex("\"(startTime|endTime)\":\\d+"), "\"$1\":<ms>")
    .replace(Regex("Gradle Test Executor \\d+"), "Gradle Test Executor <n>")

private fun BuildResult.outcomeOf(taskPath: String): TaskOutcome =
    (task(taskPath) ?: fail("No '$taskPath' in the build result")).outcome

private val BuildResult.fixtureTaskOutcome: TaskOutcome get() = outcomeOf(FIXTURE_TASK_PATH)

/**
 * The test related service messages of a build, without their [SERVICE_MESSAGE_MARKER] and with the
 * durations replaced as in [replayedTest]. Every `test...` message is taken, not only those in a
 * replay's flow, so that a message sent under the wrong flow shows up as a difference.
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

/** The flow ids the replayed messages were sent under - one per replayed test task. */
private val List<String>.flows: Set<String>
    get() = mapNotNull { FLOW_ID_ATTRIBUTE.find(it)?.groupValues?.get(1) }.toSet()

/** The flow one test was replayed under; every message of a task's replay shares it. */
private fun List<String>.flowOf(testName: String): String =
    FLOW_ID_ATTRIBUTE.find(first { testName in it })!!.groupValues[1]

/** Every flow id taken off, for comparing against the expectations, which carry none. */
private fun List<String>.withoutFlowIds(): List<String> = map { FLOW_ID_ATTRIBUTE.replace(it, "") }

private val FLOW_ID_ATTRIBUTE = Regex(" flowId='([^']*)'")

/**
 * Runs [tasks] with [cache] as the only build cache the build can reach - empty at the start of every
 * test, the remote one out of reach - so a run either executes a task or takes it from that cache.
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

/** Deletes the outputs of what the `clean...` [tasks] stand for, so the next run of those does not skip them. */
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
