/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonSlurper
import java.io.File

/**
 * What TeamCity has to be told about the tests recorded in a `test-executions.json`, so that a task
 * that never ran still reports them.
 *
 * A class of its own rather than part of [TestBuildCacheTeamCityCompatibilityService]: *what* to say
 * about a recording is the part worth testing, and a build service is not something a test can
 * instantiate, while the service is left with deciding when to say it and where it goes.
 *
 * The messages come back as the bodies of service messages, without the `##teamcity[` and `]` around
 * them. That is what lets a test hold them: a line carrying the marker would be read by the build
 * running that test as one of its own tests starting - or failing.
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
     * The tests recorded in [executionsFile], in the order they have to be sent, all under a flow of
     * this task's own so that TeamCity keeps them apart from whatever else the build runs at the
     * same time.
     *
     * A file that is missing or cannot be read is reported through [warn] and replayed as nothing: a
     * build that can no longer say what a cached task tested should not fail over it.
     */
    fun messagesFor(executionsFile: File): List<String> {
        val recorded = read(executionsFile) ?: return emptyList()
        return buildList { replay(recorded.toReplayTree(), executionsFile) }
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

    private fun MutableList<String>.replay(suite: ReplayNode, executionsFile: File) {
        for (test in suite.tests) {
            // The recorded halves joined back into the name TeamCity expects.
            val name = test.className.qualifying(test.name)
            add(serviceMessage("testStarted", "name" to name))
            when (test.status) {
                STATUS_OK -> {}
                STATUS_IGNORED -> add(serviceMessage("testIgnored", "name" to name))
                STATUS_FAILURE -> add(
                    serviceMessage(
                        "testFailed",
                        "name" to name,
                        "message" to FAILURE_DETAILS_UNAVAILABLE,
                    )
                )
                else -> warn("Unknown status '${test.status}' of test $name in $executionsFile", null)
            }
            add(serviceMessage("testFinished", "name" to name, "duration" to test.durationMillis.toString()))
        }

        for (nested in suite.suites.values) {
            add(serviceMessage("testSuiteStarted", "name" to nested.name))
            replay(nested, executionsFile)
            add(serviceMessage("testSuiteFinished", "name" to nested.name))
        }
    }

    private fun serviceMessage(messageName: String, vararg attributes: Pair<String, String>): String {
        val rendered = (attributes.toList() + ("flowId" to flowId))
            .joinToString(separator = " ") { (name, value) -> "$name='${escape(value)}'" }
        return "$messageName $rendered"
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
