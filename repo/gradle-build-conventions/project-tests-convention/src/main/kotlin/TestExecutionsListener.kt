/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import java.io.File
import java.io.FileOutputStream
import java.io.Writer
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
 * - `className` - absent when it equals the parent's, or is null on a record without a parent; otherwise present,
 *   null included. An empty string is written as reported, being Gradle's other spelling of "no class".
 * - `source` - absent for a `NoSource`. Otherwise, its `kind` - `class`, `method`, `file`, `directory`,
 *   `classpathResource` or `other` - and the fields of that kind: `className`, absent when it equals the record's,
 *   `methodName`, `file`, `classpathResourceName`, and the `line` and `column` of a position, each absent if unknown.
 *
 * A result is `resultType`, the name of Gradle's [TestResult.ResultType] (`SUCCESS`, `FAILURE` or `SKIPPED`), and the
 * `startTime` and `endTime` Gradle reports, in epoch milliseconds. A suite's time covers its own setup and teardown,
 * such as a class's `@BeforeAll` and `@AfterAll`, which no test's time includes.
 *
 * The lines are in the order Gradle reported the events, and every `parent` and `id` refers to a `suite` line above
 * it, so the file can be read in one pass. The events of the suites running in parallel forks interleave. A retried
 * test has a `test` line per attempt, in the order they ran - which is what lets TeamCity tell a flaky test from a
 * failed one. The `suiteEnd` of the outermost suite is the last line.
 */
class TestExecutionsListener(
    taskName: String,
    private val taskPath: String,
    buildDir: Provider<File>,
) : TestListener {
    val executionsFile: Provider<File> =
        buildDir.map { it.resolve("test-inventory").resolve(taskName).resolve("test-executions.jsonl") }

    @Transient
    private var recording: Recording? = null

    override fun beforeSuite(suite: TestDescriptor) {
        record { suiteRecordOf(suite) }
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        record { endSuite(suite, result) }
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {}

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        record { recordTest(testDescriptor, result) }
    }

    private fun record(action: Recording.() -> Unit) = synchronized(this) {
        val recording = recording ?: Recording(executionsFile.get(), taskPath).also { recording = it }
        recording.action()
    }

    private class SuiteRecord(val id: Int, val className: String?)

    private class Recording(private val file: File, private val taskPath: String) {
        /**
         * The suites that have started and not yet finished. Keyed by identity: Gradle passes the same descriptor to
         * `beforeSuite`, to `afterSuite` and as the `parent` of what runs in the suite, while two runs of one suite can
         * be equal in everything else.
         */
        private val running = IdentityHashMap<TestDescriptor, SuiteRecord>()
        private var lastSuiteId = 0
        private var writer: Writer? = null
        private var started = false
        private val line = StringBuilder()

        fun suiteRecordOf(suite: TestDescriptor): SuiteRecord = running[suite] ?: run {
            val parent = suite.parent?.let { suiteRecordOf(it) }
            val record = SuiteRecord(++lastSuiteId, suite.className)
            running[suite] = record
            writeLine {
                string("type", "suite")
                number("id", record.id)
                if (parent != null) number("parent", parent.id)
                descriptor(suite, parent?.className)
            }
            record
        }

        fun endSuite(suite: TestDescriptor, result: TestResult) {
            val record = suiteRecordOf(suite)
            running.remove(suite)
            writeLine {
                string("type", "suiteEnd")
                number("id", record.id)
                result(result)
            }

            if (suite.parent == null) close()
        }

        fun recordTest(test: TestDescriptor, result: TestResult) {
            val parent = test.parent?.let { suiteRecordOf(it) }
            writeLine {
                string("type", "test")
                if (parent != null) number("parent", parent.id)
                descriptor(test, parent?.className)
                result(result)
            }
        }

        private fun writeLine(members: JsonObjectWriter.() -> Unit) {
            val writer = writer ?: open()
            line.setLength(0)
            JsonObjectWriter(line).write(members)
            line.append('\n')
            writer.append(line)
        }

        private fun open(): Writer {
            file.parentFile.mkdirs()
            // Truncated by the run's first event, so that nothing of an earlier run is left in the file. Should the run
            // start over after its outermost suite has finished, what follows is appended to it.
            val writer = FileOutputStream(file, started).bufferedWriter(Charsets.UTF_8)
            this.writer = writer
            if (!started) {
                started = true
                writeLine {
                    string("type", "header")
                    number("formatVersion", 1)
                    string("taskPath", taskPath)
                }
            }
            return writer
        }

        private fun close() {
            writer?.close()
            writer = null
        }
    }
}

private fun JsonObjectWriter.descriptor(descriptor: TestDescriptor, parentClassName: String?) {
    string("name", descriptor.name)
    if (descriptor.displayName != descriptor.name) string("displayName", descriptor.displayName)
    val className = descriptor.className
    if (className != parentClassName) string("className", className)
    source(descriptor.source, className)
}

private fun JsonObjectWriter.source(source: TestSource, className: String?) {
    if (source is NoSource) return

    fun JsonObjectWriter.sourceClassName(sourceClassName: String) {
        if (sourceClassName != className) string("className", sourceClassName)
    }

    fun JsonObjectWriter.position(position: FilePosition?) {
        if (position == null) return
        number("line", position.line)
        position.column?.let { number("column", it) }
    }

    obj("source") {
        when (source) {
            is ClassSource -> {
                string("kind", "class")
                sourceClassName(source.className)
            }
            is MethodSource -> {
                string("kind", "method")
                sourceClassName(source.className)
                string("methodName", source.methodName)
            }
            is FileSource -> {
                string("kind", "file")
                string("file", source.file.path)
                position(source.position)
            }
            is DirectorySource -> {
                string("kind", "directory")
                string("file", source.file.path)
            }
            is ClasspathResourceSource -> {
                string("kind", "classpathResource")
                string("classpathResourceName", source.classpathResourceName)
                position(source.position)
            }
            // 'OtherSource', or a kind Gradle has added since.
            else -> string("kind", "other")
        }
    }
}

private fun JsonObjectWriter.result(result: TestResult) {
    string("resultType", result.resultType.name)
    number("startTime", result.startTime)
    number("endTime", result.endTime)
}

/** Writes a JSON object into [out], one member at a time. */
private class JsonObjectWriter(private val out: StringBuilder) {
    private var empty = true

    fun write(members: JsonObjectWriter.() -> Unit) {
        out.append('{')
        members()
        out.append('}')
    }

    fun string(key: String, value: String?) {
        key(key)
        if (value == null) out.append("null") else out.appendJsonString(value)
    }

    fun number(key: String, value: Number) {
        key(key)
        out.append(value)
    }

    fun obj(key: String, members: JsonObjectWriter.() -> Unit) {
        key(key)
        JsonObjectWriter(out).write(members)
    }

    private fun key(key: String) {
        if (!empty) out.append(',')
        empty = false
        out.appendJsonString(key).append(':')
    }
}

/** Appends [value] as a JSON string literal, so that no test name can break out of it, nor lose a character. */
private fun StringBuilder.appendJsonString(value: String): StringBuilder {
    append('"')
    for ((index, char) in value.withIndex()) {
        when {
            char == '"' -> append("\\\"")
            char == '\\' -> append("\\\\")
            char == '\n' -> append("\\n")
            char == '\r' -> append("\\r")
            char == '\t' -> append("\\t")
            char < ' ' -> appendUnicodeEscape(char)
            // Half a surrogate pair has no UTF-8 encoding, so it is escaped rather than replaced when the file is written.
            char.isHighSurrogate() && value.getOrNull(index + 1)?.isLowSurrogate() != true -> appendUnicodeEscape(char)
            char.isLowSurrogate() && value.getOrNull(index - 1)?.isHighSurrogate() != true -> appendUnicodeEscape(char)
            else -> append(char)
        }
    }
    append('"')
    return this
}

private fun StringBuilder.appendUnicodeEscape(char: Char) {
    append("\\u").append(char.code.toString(16).padStart(4, '0'))
}
