/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonSlurper
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.File

/**
 * Reports the tests of a test task that TeamCity would otherwise never hear about.
 *
 * TeamCity learns which tests ran from the events its own Gradle integration observes while the tests
 * execute. A task served from the build cache executes nothing, so it reports nothing, and everything
 * that builds on test statistics - Parallel Tests above all - silently loses that task. This service
 * closes the gap by replaying the tests recorded in `test-executions.json` by [TestExecutionsListener]
 * as TeamCity service messages, which is possible because that file is a declared output of the task
 * and is therefore restored from the cache along with the rest of them.
 *
 * Registered only for a TeamCity build, as nothing elsewhere listens to service messages, so this
 * service can assume its output has a reader.
 *
 * Registered as a build service (rather than a plain object) because [OperationCompletionListener]
 * implementations must be build services to be accepted by
 * [org.gradle.build.event.BuildEventsListenerRegistry.onTaskCompletion].
 */
abstract class TestBuildCacheTeamCityCompatibilityService :
    BuildService<TestBuildCacheTeamCityCompatibilityService.Parameters>,
    OperationCompletionListener {

    interface Parameters : BuildServiceParameters {
        /**
         * Path of the test task this service watches, e.g. `:compiler:test`. A task finish event carries
         * only a task path, never a task type, so the path has to be recorded while the task is configured.
         */
        val taskPath: Property<String>

        /**
         * The `test-executions.json` file the watched task declares as an output: the executed tests
         * with their suite nesting preserved, which is what replaying them to TeamCity as suite and
         * test service messages needs.
         *
         * Passed as a *parameter* on purpose: build service parameters are stored in the configuration
         * cache, while build service state is not, so anything kept in a field would silently be empty
         * on a configuration cache hit.
         */
        val executionsFile: Property<File>
    }

    private companion object {
        // The statuses TestExecutionsListener writes, see TestResult.statusName.
        const val STATUS_OK = "OK"
        const val STATUS_FAILURE = "Failure"
        const val STATUS_IGNORED = "Ignored"

        /**
         * A test only reaches this service as failed when the task around it still succeeded, which
         * means the failure was muted or tolerated. The recorded file keeps the status but not the
         * stack trace, so the message says where the verdict comes from instead of inventing details.
         */
        const val FAILURE_DETAILS_UNAVAILABLE =
            "Test failed when this task last ran; replayed from cached test results, no failure details recorded"
    }

    private val log = Logging.getLogger(javaClass)

    // Resolved once: 'parameters' are final by the time the first event arrives.
    private val taskPath: String by lazy { parameters.taskPath.get() }
    private val executionsFile: File by lazy { parameters.executionsFile.get() }

    /**
     * Keeps the replayed messages of this task apart from every other message in the build.
     *
     * TeamCity infers the suite a test belongs to from the messages that are currently open *in the
     * same flow*, and Gradle replays several cached test tasks at once. Without a flow of its own, one
     * task's `testSuiteStarted` would end up enclosing another task's tests, which TeamCity then
     * registers under a suite path it never belonged to.
     *
     * The task path is unique within a build and stable for the whole replay, which is exactly what a
     * flow id has to be. It is prefixed so that it cannot coincide with a flow id of the TeamCity
     * Gradle runner itself.
     */
    private val flowId: String by lazy { "TestReplay$taskPath" }

    // Note: 'onFinish' receives the general 'FinishEvent'. Only task events are of interest here,
    // so everything else (e.g. transform or test events) is filtered out.
    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return

        // Events are delivered for the whole build, so every task other than the watched one is skipped.
        if (event.descriptor.taskPath != taskPath) return

        // A task that failed or was skipped has no complete set of test results to report.
        val result = event.result as? TaskSuccessResult ?: return

        // Only a task that did not run needs its tests replayed: when it really executed, TeamCity has
        // already seen every test, and replaying would report each of them a second time.
        if (!result.isFromCache && !result.isUpToDate) return

        val suite = readExecutions() ?: return
        replay(suite)
    }

    /**
     * Reads the recorded tests, or returns `null` if they cannot be read.
     *
     * The whole document is turned into [RecordedSuite]s before anything is printed, so that a file
     * that turns out to be unreadable cannot leave half-emitted, unbalanced suite messages behind.
     * Nothing here fails the build: these messages carry statistics, and losing them for one task is a
     * far better outcome than breaking a build that has otherwise succeeded.
     */
    private fun readExecutions(): RecordedSuite? {
        if (!executionsFile.isFile) {
            log.warn("No test executions recorded for $taskPath: $executionsFile does not exist")
            return null
        }

        return runCatching {
            val document = JsonSlurper().parse(executionsFile, Charsets.UTF_8.name()) as Map<*, *>
            document.toRecordedSuite(name = "")
        }.getOrElse { failure ->
            log.warn("Cannot replay the tests of $taskPath from $executionsFile", failure)
            null
        }
    }

    private fun replay(suite: RecordedSuite) {
        // The root suite is only a container for the top level suites and for tests reported outside
        // any suite, so it is not announced to TeamCity itself.
        for (test in suite.tests) {
            serviceMessage("testStarted", "name" to test.name)
            when (test.status) {
                STATUS_OK -> {}
                STATUS_IGNORED -> serviceMessage("testIgnored", "name" to test.name)
                STATUS_FAILURE -> serviceMessage(
                    "testFailed",
                    "name" to test.name,
                    "message" to FAILURE_DETAILS_UNAVAILABLE,
                )
                else -> log.warn("Unknown status '${test.status}' of test ${test.name} in $executionsFile")
            }
            serviceMessage("testFinished", "name" to test.name, "duration" to test.durationMillis.toString())
        }

        for (nested in suite.suites) {
            serviceMessage("testSuiteStarted", "name" to nested.name)
            replay(nested)
            serviceMessage("testSuiteFinished", "name" to nested.name)
        }
    }

    private fun serviceMessage(messageName: String, vararg attributes: Pair<String, String>) {
        val rendered = (attributes.toList() + ("flowId" to flowId))
            .joinToString(separator = " ") { (name, value) -> "$name='${escape(value)}'" }
        // Printed rather than logged, as the other service messages of this build are, see for instance
        // 'DexMethodCount' and the cache-redirector settings plugin.
        println("##teamcity[$messageName $rendered]")
    }

    /**
     * Escapes [value] for a service message attribute.
     *
     * `|` is escaped first, so that the escape character this very function introduces is not escaped
     * again. See https://www.jetbrains.com/help/teamcity/service-messages.html
     */
    private fun escape(value: String): String = value
        .replace("|", "||")
        .replace("'", "|'")
        .replace("\n", "|n")
        .replace("\r", "|r")
        .replace("[", "|[")
        .replace("]", "|]")

    private class RecordedSuite(val name: String, val suites: List<RecordedSuite>, val tests: List<RecordedTest>)

    private class RecordedTest(val name: String, val status: String, val durationMillis: Long)

    private fun Map<*, *>.toRecordedSuite(name: String): RecordedSuite = RecordedSuite(
        name = name,
        suites = members("suites").map { it.toRecordedSuite(it.string("name")) },
        tests = members("tests").map { RecordedTest(it.string("name"), it.string("status"), it.long("duration")) },
    )

    private fun Map<*, *>.members(key: String): List<Map<*, *>> =
        (this[key] as? List<*> ?: emptyList<Any>()).map { it as Map<*, *> }

    private fun Map<*, *>.string(key: String): String = this[key] as String

    private fun Map<*, *>.long(key: String): Long = (this[key] as Number).toLong()
}
