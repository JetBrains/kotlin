/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonSlurper
import java.io.File

/**
 * What TeamCity is told about the tests recorded in a `test-executions.json`, so that a task that
 * never ran still reports them.
 *
 * Apart from [TestBuildCacheTeamCityCompatibilityService] because a build service is not something
 * a test can instantiate. The messages are bodies, without the `##teamcity[` and `]`: a whole line
 * would be read by the build running a test of this as one of its own tests.
 */
internal class TestExecutionsReplay(
    private val taskPath: String,
    private val warn: (String, Throwable?) -> Unit,
) {
    private companion object {
        const val STATUS_OK = "OK"
        const val STATUS_FAILURE = "Failure"
        const val STATUS_IGNORED = "Ignored"
        const val FAILURE_DETAILS_UNAVAILABLE =
            "Test failed when this task last ran; replayed from cached test results, no failure details recorded"
    }

    /**
     * The tests recorded in [executionsFile], in the order they have to be sent, under a flow of
     * this task's own so that TeamCity keeps them apart from whatever runs alongside.
     *
     * Produced as they are asked for, so that a task's tests are never all held at once - the
     * largest of them run six figures of them. The file is read when the sequence is first iterated.
     *
     * A file that is missing or unreadable is reported through [warn] and replayed as nothing: not
     * being able to say what a cached task tested is no reason to fail the build.
     */
    fun messagesFor(executionsFile: File): Sequence<String> = sequence {
        val recorded = read(executionsFile) ?: return@sequence
        replay(recorded.toReplayTree(), executionsFile)
    }

    private val flowId = "TestReplay$taskPath"

    private fun read(executionsFile: File): RecordedSuite? {
        if (!executionsFile.isFile) {
            warn("No test executions recorded for $taskPath: $executionsFile does not exist", null)
            return null
        }

        return runCatching {
            val document = JsonSlurper().parse(executionsFile, Charsets.UTF_8.name()) as Map<*, *>
            document.toRecordedSuite(suiteName = "")
        }.getOrElse { failure ->
            warn("Cannot replay the tests of $taskPath from $executionsFile", failure)
            null
        }
    }

    /**
     * Rebuilds the suite nesting TeamCity expects from the structure Gradle reported.
     *
     * TeamCity spells a test `<class>.<method>` and does not repeat the class as a suite, so the
     * class suite the recording keeps for its timings is collapsed here - per test, not per suite: a
     * class with nested classes stays a suite for *their* tests while disappearing for its own.
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

    private suspend fun SequenceScope<String>.replay(suite: ReplayNode, executionsFile: File) {
        for (test in suite.tests) {
            // The recorded halves joined back into the name TeamCity expects.
            val name = test.className.qualifying(test.name)
            val outcome = when (test.status) {
                STATUS_OK -> null
                STATUS_IGNORED -> serviceMessage("testIgnored", "name" to name)
                STATUS_FAILURE -> serviceMessage(
                    "testFailed",
                    "name" to name,
                    "message" to FAILURE_DETAILS_UNAVAILABLE,
                )
                // Not replayed at all: to TeamCity a test that starts and finishes saying nothing
                // else has passed, and a status this build cannot read is no reason to claim that.
                // Recordings do cross versions of this code - the build cache is shared by branches.
                else -> {
                    warn("Unknown status '${test.status}' of test $name in $executionsFile", null)
                    continue
                }
            }

            yield(serviceMessage("testStarted", "name" to name))
            if (outcome != null) yield(outcome)
            yield(serviceMessage("testFinished", "name" to name, "duration" to test.durationMillis.toString()))
        }

        for (nested in suite.suites.values) {
            yield(serviceMessage("testSuiteStarted", "name" to nested.name))
            replay(nested, executionsFile)
            yield(serviceMessage("testSuiteFinished", "name" to nested.name))
        }
    }

    private fun serviceMessage(messageName: String, vararg attributes: Pair<String, String>): String {
        val rendered = (attributes.toList() + ("flowId" to flowId))
            .joinToString(separator = " ") { (name, value) -> "$name='${escape(value)}'" }
        return "$messageName $rendered"
    }

    /**
     * Escapes [value] for a service message attribute, `|` first so that the escape character this
     * adds is not escaped again. See https://www.jetbrains.com/help/teamcity/service-messages.html
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
        // An absent 'className' means the enclosing suite is the test's class, an explicit null
        // that it has none; the root suite's empty name stands for no class.
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
