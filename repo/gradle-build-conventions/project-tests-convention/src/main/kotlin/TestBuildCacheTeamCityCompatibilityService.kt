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

        val suite = readExecutions() ?: return
        replay(suite)
    }

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
