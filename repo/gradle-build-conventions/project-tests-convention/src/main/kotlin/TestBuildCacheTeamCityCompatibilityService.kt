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
 * Reports the tests of a test task served from the Gradle Build Cache to TeamCity
 */
abstract class TestBuildCacheTeamCityCompatibilityService :
    BuildService<TestBuildCacheTeamCityCompatibilityService.Parameters>,
    OperationCompletionListener {

    interface Parameters : BuildServiceParameters {
        val taskPath: Property<String>
        val executionsFile: Property<File>
    }

    private companion object {
        const val STATUS_OK = "OK"
        const val STATUS_FAILURE = "Failure"
        const val STATUS_IGNORED = "Ignored"
        const val FAILURE_DETAILS_UNAVAILABLE =
            "Test failed when this task last ran; replayed from cached test results, no failure details recorded"
    }

    private val log = Logging.getLogger(javaClass)
    private val taskPath: String by lazy { parameters.taskPath.get() }
    private val executionsFile: File by lazy { parameters.executionsFile.get() }
    private val flowId: String by lazy { "TestReplay$taskPath" }

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return
        if (event.descriptor.taskPath != taskPath) return
        val result = event.result as? TaskSuccessResult ?: return
        if (!result.isFromCache && !result.isUpToDate) return

        val recorded = readExecutions() ?: return
        replay(recorded.toReplayTree())
    }

    private fun readExecutions(): RecordedSuite? {
        if (!executionsFile.isFile) {
            log.warn("No test executions recorded for $taskPath: $executionsFile does not exist")
            return null
        }

        return runCatching {
            val document = JsonSlurper().parse(executionsFile, Charsets.UTF_8.name()) as Map<*, *>
            document.toRecordedSuite(suiteName = "")
        }.getOrElse { failure ->
            log.warn("Cannot replay the tests of $taskPath from $executionsFile", failure)
            null
        }
    }

    /**
     * Rebuilds the suite nesting TeamCity expects from the structure Gradle reported.
     *
     * The recorded file keeps every suite Gradle reports, including the one standing for a test's own
     * class, because that is where the per-class timings hang. TeamCity names a test `<class>.<method>`
     * and does not repeat the class as an enclosing suite, so those are collapsed again here - per
     * test, not per suite: a class that also has nested classes stays a suite for *their* tests while
     * disappearing for its own.
     */
    private fun RecordedSuite.toReplayTree(): ReplayNode {
        val replayRoot = ReplayNode("")

        fun collect(node: RecordedSuite, path: List<String>) {
            for (test in node.tests) {
                val teamCityPath = test.className?.let { path.dropLastWhile { name -> name == it } } ?: path
                val target = teamCityPath.fold(replayRoot) { parent, name ->
                    parent.suites.getOrPut(name) { ReplayNode(name) }
                }
                target.tests += test
            }
            for (child in node.suites) collect(child, path + child.name)
        }

        collect(this, emptyList())
        return replayRoot
    }

    private fun replay(suite: ReplayNode) {
        for (test in suite.tests) {
            // The recorded halves joined back into the name TeamCity expects.
            val name = test.className.qualifying(test.name)
            serviceMessage("testStarted", "name" to name)
            when (test.status) {
                STATUS_OK -> {}
                STATUS_IGNORED -> serviceMessage("testIgnored", "name" to name)
                STATUS_FAILURE -> serviceMessage(
                    "testFailed",
                    "name" to name,
                    "message" to FAILURE_DETAILS_UNAVAILABLE,
                )
                else -> log.warn("Unknown status '${test.status}' of test $name in $executionsFile")
            }
            serviceMessage("testFinished", "name" to name, "duration" to test.durationMillis.toString())
        }

        for (nested in suite.suites.values) {
            serviceMessage("testSuiteStarted", "name" to nested.name)
            replay(nested)
            serviceMessage("testSuiteFinished", "name" to nested.name)
        }
    }

    private fun serviceMessage(messageName: String, vararg attributes: Pair<String, String>) {
        val rendered = (attributes.toList() + ("flowId" to flowId))
            .joinToString(separator = " ") { (name, value) -> "$name='${escape(value)}'" }
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

    private class RecordedTest(
        val name: String,
        val className: String?,
        val status: String,
        val durationMillis: Long,
    )

    /** The suite nesting as TeamCity wants it, rebuilt from the recorded one. */
    private class ReplayNode(val name: String) {
        val suites = LinkedHashMap<String, ReplayNode>()
        val tests = mutableListOf<RecordedTest>()
    }

    private fun Map<*, *>.toRecordedSuite(suiteName: String): RecordedSuite = RecordedSuite(
        name = suiteName,
        suites = members("suites").map { it.toRecordedSuite(it.string("name")) },
        // An absent 'className' means the enclosing suite is the test's class; an explicit null means
        // the test has none. The root suite's name is empty and stands for no class.
        tests = members("tests").map {
            val className = if (it.containsKey("className")) it["className"] as String? else suiteName.ifEmpty { null }
            RecordedTest(it.string("name"), className, it.string("status"), it.long("duration"))
        },
    )

    private fun Map<*, *>.members(key: String): List<Map<*, *>> =
        (this[key] as? List<*> ?: emptyList<Any>()).map { it as Map<*, *> }

    private fun Map<*, *>.string(key: String): String = this[key] as String

    private fun Map<*, *>.long(key: String): Long = (this[key] as Number).toLong()
}
