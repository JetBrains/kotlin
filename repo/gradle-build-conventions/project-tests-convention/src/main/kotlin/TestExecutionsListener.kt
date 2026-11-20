/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import groovy.json.JsonOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.source.ClassSource
import org.gradle.api.tasks.testing.source.ClasspathResourceSource
import org.gradle.api.tasks.testing.source.DirectorySource
import org.gradle.api.tasks.testing.source.FilePosition
import org.gradle.api.tasks.testing.source.FileSource
import org.gradle.api.tasks.testing.source.MethodSource
import org.gradle.api.tasks.testing.source.NoSource
import org.gradle.api.tasks.testing.source.TestSource
import java.io.Closeable
import java.io.File
import java.util.IdentityHashMap

/**
 * Records the test events of a test task to `test-executions.jsonl`, as Gradle reports them, one JSON object per line
 * written as the event arrives.
 *
 * The file is a declared output of the test task, so the build cache restores it along with the task. It is the raw
 * material for the test inventory, for the test durations that test sharding balances by, and for reporting the tests
 * of a task served from the build cache to TeamCity.
 *
 * ### Format
 *
 * ```
 * {"type":"header","formatVersion":1,"taskPath":":native:unsafe-mem:test"}
 * {"type":"suite","id":1,"name":"Gradle Test Run :native:unsafe-mem:test"}
 * {"type":"suite","id":2,"parent":1,"name":"Gradle Test Executor 3"}
 * {"type":"suite","id":3,"parent":2,"name":"org.example.MemTest","displayName":"MemTest","className":"org.example.MemTest","source":{"kind":"class"}}
 * {"type":"test","parent":3,"name":"reads()","source":{"kind":"method","methodName":"reads"},"resultType":"SUCCESS","startTime":1760951234601,"endTime":1760951234604}
 * {"type":"suiteEnd","id":3,"resultType":"SUCCESS","startTime":1760951234590,"endTime":1760951234634}
 * ```
 *
 * Records, by `type`:
 * - `header` - the first line: `formatVersion` and the `taskPath` of the task recorded.
 * - `suite` - a suite has started: its `id`, unique within the file, the `id` of its `parent`, absent for the outermost
 *   suite, and its descriptor. Every suite Gradle reports is recorded, those it inserts itself included: the test run,
 *   the test executors, which tell the forks and the rounds of a retry apart, and the partitions. A suite run again,
 *   such as a class retried under a new executor, is a new suite with an `id` of its own.
 * - `test` - a test has finished: its `parent`, its descriptor and its result. Nothing refers to a test, so it has no `id`.
 * - `suiteEnd` - the suite with this `id` has finished, and its result.
 *
 * A descriptor is what Gradle's [TestDescriptor] contains, shortened if nothing is lost:
 * - `name`.
 * - `displayName` - absent when it equals `name`.
 * - `className` - absent when it equals the parent's, taken as null for the outermost suite; otherwise written as
 *   reported, null and the empty string included, which are Gradle's two spellings of "no class".
 * - `source` - absent for a `NoSource`. Otherwise, its `kind` - `class`, `method`, `file`, `directory`,
 *   `classpathResource` or `other` - and the fields of that kind: `className`, absent when it equals the record's,
 *   `methodName`, `file`, `classpathResourceName`, and the `line` and `column` of a position, each absent if unknown.
 *
 * A result is `resultType`, the name of Gradle's [TestResult.ResultType] (`SUCCESS`, `FAILURE` or `SKIPPED`), and the
 * `startTime` and `endTime` Gradle reports, in epoch milliseconds. A suite's time covers its own setup and teardown,
 * such as a class's `@BeforeAll` and `@AfterAll`, which no test's time includes. Failure details are not recorded: no
 * messages or stack traces go into a cached artifact.
 *
 * The lines are in the order Gradle reported the events, and every `parent` and `id` refers to a `suite` line above
 * it, so the file can be read in one pass. The events of the suites running in parallel forks interleave. A retried
 * test has a `test` line per attempt, in the order they ran - which is what lets TeamCity tell a flaky test from a
 * failed one. The `suiteEnd` of the outermost suite is the last line.
 *
 * Strings have every character outside printable ASCII escaped, so the file is ASCII and keeps any name intact, half a
 * surrogate pair included.
 */
class TestExecutionsListener(
    taskName: String,
    private val taskPath: String,
    buildDir: Provider<File>,
) : TestListener {
    val executionsFile: Provider<File> =
        buildDir.map { it.resolve("test-inventory").resolve(taskName).resolve("test-executions.jsonl") }

    /**
     * The test run of the task, from the start of its outermost suite, which Gradle reports before anything else, to
     * its end. A task execution has a single one: Gradle removes the listeners of the task once its tests have run.
     */
    @Transient
    private lateinit var recording: Recording

    // Gradle reports the events one at a time for now, but asks listeners not to count on it.
    @Synchronized
    override fun beforeSuite(suite: TestDescriptor) {
        if (suite.parent == null) {
            recording = Recording(executionsFile.get())
            recording.writeHeader(taskPath)
        }
        recording.startSuite(suite)
    }

    @Synchronized
    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        recording.endSuite(suite, result)
        if (suite.parent == null) recording.close()
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {}

    @Synchronized
    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        recording.recordTest(testDescriptor, result)
    }

    private class SuiteRecord(val id: Int, val className: String?)

    private class Recording(file: File) : Closeable {
        /**
         * The suites that have started and not yet finished. Keyed by identity: Gradle passes the same descriptor to
         * `beforeSuite`, to `afterSuite` and as the `parent` of what runs in the suite, while two runs of one suite can
         * be equal in everything else.
         */
        private val running = IdentityHashMap<TestDescriptor, SuiteRecord>()
        private var lastSuiteId = 0

        // Truncated when opened, so that nothing of an earlier run is left in the file.
        private val writer by lazy { file.run { parentFile.mkdirs(); bufferedWriter(Charsets.UTF_8) } }

        fun writeHeader(taskPath: String) {
            writeLine {
                put("type", "header")
                put("formatVersion", 1)
                put("taskPath", taskPath)
            }
        }

        fun startSuite(suite: TestDescriptor) {
            val parent = suite.parent?.let { running.getValue(it) }
            val record = SuiteRecord(++lastSuiteId, suite.className)
            running[suite] = record
            writeLine {
                put("type", "suite")
                put("id", record.id)
                if (parent != null) put("parent", parent.id)
                putDescriptor(suite, parent?.className)
            }
        }

        fun endSuite(suite: TestDescriptor, result: TestResult) {
            val record = running.remove(suite)!!
            writeLine {
                put("type", "suiteEnd")
                put("id", record.id)
                putResult(result)
            }
        }

        fun recordTest(test: TestDescriptor, result: TestResult) {
            val parent = test.parent?.let { running.getValue(it) }
            writeLine {
                put("type", "test")
                if (parent != null) put("parent", parent.id)
                putDescriptor(test, parent?.className)
                putResult(result)
            }
        }

        /** Writes a record of [members], in the order they are put. */
        private fun writeLine(members: MutableMap<String, Any?>.() -> Unit) {
            writer.appendLine(JsonOutput.toJson(buildMap(members)))
        }

        override fun close() = writer.close()
    }
}

private fun MutableMap<String, Any?>.putDescriptor(descriptor: TestDescriptor, parentClassName: String?) {
    put("name", descriptor.name)
    if (descriptor.displayName != descriptor.name) put("displayName", descriptor.displayName)
    val className = descriptor.className
    if (className != parentClassName) put("className", className)
    putSource(descriptor.source, className)
}

private fun MutableMap<String, Any?>.putSource(source: TestSource, className: String?) {
    if (source is NoSource) return

    fun MutableMap<String, Any>.putClassName(sourceClassName: String) {
        if (sourceClassName != className) put("className", sourceClassName)
    }

    fun MutableMap<String, Any>.putPosition(position: FilePosition?) {
        if (position == null) return
        put("line", position.line)
        position.column?.let { put("column", it) }
    }

    put("source", buildMap {
        when (source) {
            is ClassSource -> {
                put("kind", "class")
                putClassName(source.className)
            }
            is MethodSource -> {
                put("kind", "method")
                putClassName(source.className)
                put("methodName", source.methodName)
            }
            is FileSource -> {
                put("kind", "file")
                put("file", source.file.path)
                putPosition(source.position)
            }
            is DirectorySource -> {
                put("kind", "directory")
                put("file", source.file.path)
            }
            is ClasspathResourceSource -> {
                put("kind", "classpathResource")
                put("classpathResourceName", source.classpathResourceName)
                putPosition(source.position)
            }
            // 'OtherSource', or a kind Gradle has added since.
            else -> put("kind", "other")
        }
    })
}

private fun MutableMap<String, Any?>.putResult(result: TestResult) {
    put("resultType", result.resultType.name)
    put("startTime", result.startTime)
    put("endTime", result.endTime)
}
