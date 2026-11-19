/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonSlurper
import java.io.File

/**
 * What TeamCity is told about the tests recorded in a `test-executions.jsonl`, so that a task that
 * never ran still reports them.
 *
 * The recording is what [TestExecutionsListener] wrote, one JSON object per line as Gradle reported
 * the events, suites and all; this is the other side of that, reading it back. The suite nesting is
 * rebuilt as TeamCity expects it, which is not the nesting Gradle reports: the suites Gradle inserts
 * itself are gone, and a class is part of a test's name rather than a suite. Nothing is emitted
 * for a suite that has no tests and no suite under it: a replay holds back a recording's empty
 * suites, because a suite reaches the messages only by a test of its own.
 *
 * Apart from [TestBuildCacheTeamCityCompatibilityService] because a build service is not something
 * a test can instantiate. The messages are bodies, without the `##teamcity[` and `]`: a whole line
 * would be read by the build running a test of this as one of its own tests.
 *
 * [flowId] is the name of the flow every message is sent under, which keeps a replay apart from
 * whatever runs alongside it. It is a label and nothing more - nothing outside the build it was
 * sent in ever refers to it - so [TestBuildCacheTeamCityCompatibilityService] hands each task a
 * short numbered one: a replay can run six figures of messages, and the flow id rides on every
 * one of them, which is a fifth of the bytes a replay writes.
 */
internal class TestExecutionsReplay(
    private val taskPath: String,
    private val flowId: String,
    private val warn: (String, Throwable?) -> Unit,
) {
    private companion object {
        /** The only recording format this can replay, the `formatVersion` of a `header` record. */
        const val FORMAT_VERSION = 1

        const val RESULT_SUCCESS = "SUCCESS"
        const val RESULT_FAILURE = "FAILURE"
        const val RESULT_SKIPPED = "SKIPPED"
        const val FAILURE_DETAILS_UNAVAILABLE =
            "Test failed when this task last ran; replayed from cached test results, no failure details recorded"
    }

    /**
     * The tests recorded in [executionsFile], in the order they have to be sent, under this
     * replay's [flowId] so that TeamCity keeps them apart from whatever runs alongside.
     *
     * The file is read and parsed when the sequence is first iterated, all of it before anything is
     * emitted: a recording that turns out unreadable partway through is not worth half a replay.
     * Nothing is held beyond the parse - the tree the messages are emitted from, not the records -
     * so the largest of them, which run six figures of tests, is held once, not twice.
     *
     * A file that is missing or unreadable is reported through [warn] and replayed as nothing: not
     * being able to say what a cached task tested is no reason to fail the build.
     */
    fun messagesFor(executionsFile: File): Sequence<String> = sequence {
        val replayTree = read(executionsFile) ?: return@sequence
        replay(replayTree)
    }

    /** The runner drops suites named after the task, and a task is named by its last path segment. */
    private val taskName = taskPath.substringAfterLast(':')

    private fun read(executionsFile: File): ReplayNode? {
        if (!executionsFile.isFile) {
            warn("No test executions recorded for $taskPath: $executionsFile does not exist", null)
            return null
        }

        return runCatching { parse(executionsFile) }.getOrElse { failure ->
            warn("Cannot replay the tests of $taskPath from $executionsFile", failure)
            null
        }
    }

    /**
     * Reads the whole recording into the tree the messages are emitted from, one record at a time.
     *
     * Every record of the file, including a record type this does not know, is read by the same
     * rules: `formatVersion` is what says a recording can be made sense of, so an unknown record of
     * a known version is ignored rather than refused - the build cache is shared by branches, and
     * recordings cross versions of this code.
     */
    private fun parse(executionsFile: File): ReplayNode {
        val replayRoot = ReplayNode("")
        val runningSuites = HashMap<Int, RecordedSuite>()

        executionsFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val record = JsonSlurper().parseText(line) as Map<*, *>
                when (record.string("type")) {
                    // The suite a `parent` refers to has always started above, so it is still open.
                    "suite" -> {
                        val parent = record.parent(runningSuites)
                        runningSuites[record.int("id")] =
                            RecordedSuite(record.string("name"), record.className(parent), parent)
                    }
                    "suiteEnd" -> runningSuites.remove(record.int("id"))
                    "test" -> recordTest(record, record.parent(runningSuites), replayRoot, executionsFile)
                    "header" -> {
                        val formatVersion = record.int("formatVersion")
                        if (formatVersion != FORMAT_VERSION) {
                            error("$executionsFile is recorded in format version $formatVersion, which this cannot replay")
                        }
                    }
                    // A record type this does not know is left alone: the format version is what
                    // says a recording can be made sense of, so an unknown record of a known
                    // version is an addition, not a change.
                }
            }
        }
        return replayRoot
    }

    /**
     * Puts one recorded test where it belongs in the tree, its TeamCity name computed from the
     * nesting Gradle reported: the suites that carry nothing of the name dropped, the class part of
     * the name, and what is left the suites the test sits under.
     *
     * The class suite the recording keeps for its timings is collapsed per test, not per suite: a
     * class with nested classes stays a suite for *their* tests while disappearing for its own.
     */
    private fun recordTest(record: Map<*, *>, parent: RecordedSuite?, replayRoot: ReplayNode, executionsFile: File) {
        val className = record.className(parent)
        val name = teamCityRunnerMethodName(record.string("name"), record.displayName())
        val testName = className.qualifying(name)

        val reportedSuites = generateSequence(parent) { it.parent }.map { it.name }.toList().asReversed()
        val teamCityPath = (className?.let { reportedSuites.dropLastWhile { suite -> suite == it } } ?: reportedSuites)
            .filterNot { isSyntheticSuiteName(it, taskName, className) }

        val target = teamCityPath.fold(replayRoot) { suite, suiteName ->
            suite.suites.getOrPut(suiteName) { ReplayNode(suiteName) }
        }
        target.tests += RecordedTest(
            testName,
            record.string("resultType"),
            (record["endTime"] as Number).toLong() - (record["startTime"] as Number).toLong(),
            executionsFile,
        )
    }

    private suspend fun SequenceScope<String>.replay(suite: ReplayNode) {
        for (test in suite.tests) {
            // The recorded halves joined back into the name TeamCity expects.
            val outcome = when (test.resultType) {
                RESULT_SUCCESS -> null
                RESULT_SKIPPED -> serviceMessage("testIgnored", "name" to test.name)
                RESULT_FAILURE -> serviceMessage(
                    "testFailed",
                    "name" to test.name,
                    "message" to FAILURE_DETAILS_UNAVAILABLE,
                )
                // Not replayed at all: to TeamCity a test that starts and finishes saying nothing
                // else has passed, and a result this build cannot read is no reason to claim that.
                // Recordings do cross versions of this code - the build cache is shared by branches.
                else -> {
                    warn("Unknown result type '${test.resultType}' of test ${test.name} in ${test.executionsFile}", null)
                    continue
                }
            }

            yield(serviceMessage("testStarted", "name" to test.name))
            if (outcome != null) yield(outcome)
            yield(serviceMessage("testFinished", "name" to test.name, "duration" to test.durationMillis.toString()))
        }

        for (nested in suite.suites.values) {
            yield(serviceMessage("testSuiteStarted", "name" to nested.name))
            replay(nested)
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

    /** A suite that has started and not ended. */
    private class RecordedSuite(val name: String, val className: String?, val parent: RecordedSuite?)

    private class RecordedTest(
        val name: String,
        val resultType: String,
        val durationMillis: Long,
        val executionsFile: File,
    )

    /** The suite nesting as TeamCity wants it, rebuilt from the recorded one. */
    private class ReplayNode(val name: String) {
        val suites = LinkedHashMap<String, ReplayNode>()
        val tests = mutableListOf<RecordedTest>()
    }

    private fun Map<*, *>.string(key: String): String = this[key] as String? ?: error("A record has no '$key'")

    private fun Map<*, *>.int(key: String): Int = (this[key] as Number).toInt()

    /** A record's `displayName`, which a recording omits when it is the name itself. */
    private fun Map<*, *>.displayName(): String = this["displayName"] as String? ?: string("name")

    /**
     * The `parent` of a record as the suite it refers to. A `parent` outside the running suites is
     * a recording this cannot follow; `getValue` says so.
     */
    private fun Map<*, *>.parent(runningSuites: Map<Int, RecordedSuite>): RecordedSuite? =
        this["parent"]?.let { runningSuites.getValue((it as Number).toInt()) }

    /**
     * The class a record belongs to: written only where it is not the parent suite's, an explicit
     * null being Gradle's way of saying a test has no class at all.
     */
    private fun Map<*, *>.className(parent: RecordedSuite?): String? =
        if (containsKey("className")) this["className"] as String? else parent?.className
}
