/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.io.File

/**
 * Writes the executed tests to `test-executions.json`, suite nesting intact, so that the tests of a
 * task served from the build cache can be replayed to TeamCity as service messages.
 *
 * The structured counterpart of [TestInventoryListener]: the flat `test-inventory.tsv` joins the
 * suite names and the test name with `": "`, which cannot be undone. Test names differ from the
 * inventory's too - recorded in the form TeamCity's own runner sends, which the server then
 * normalizes, see [teamCityRunnerMethodName].
 *
 * ### Format
 *
 * The file is a declared output of the test task, so the build cache restores it - which is what
 * lets a task that never ran still report its tests.
 *
 * A recording of `:kotlin-util-klib-abi:test`, abridged to one test per suite and two of its nine
 * top level suites - a plain test class, and one whose tests sit a suite deeper:
 *
 * ```json
 * {
 *   "formatVersion": 1,
 *   "taskPath": ":kotlin-util-klib-abi:test",
 *   "duration": 5398,
 *   "suites": [
 *     {
 *       "name": "org.jetbrains.kotlin.library.abi.parser.CursorTest",
 *       "duration": 44,
 *       "suites": [],
 *       "tests": [
 *         { "name": "skipWhitespace()", "status": "OK", "duration": 0 }
 *       ]
 *     },
 *     {
 *       "name": "org.jetbrains.kotlin.library.abi.AbiTypeArgumentRenderingTest",
 *       "duration": 115,
 *       "suites": [
 *         {
 *           "name": "test$org_jetbrains_kotlin_kotlin_util_klib_abi_test(Supported)",
 *           "duration": 95,
 *           "suites": [],
 *           "tests": [
 *             {
 *               "name": "test$org_jetbrains_kotlin_kotlin_util_klib_abi_test([1] V1)",
 *               "className": "org.jetbrains.kotlin.library.abi.AbiTypeArgumentRenderingTest",
 *               "status": "OK",
 *               "duration": 32
 *             }
 *           ]
 *         }
 *       ],
 *       "tests": []
 *     }
 *   ],
 *   "tests": []
 * }
 * ```
 *
 * The second suite is a class whose tests are not directly in it: JUnit 5 reports a parameterized
 * method as a container of its own, and each invocation names the class it belongs to.
 *
 * - `formatVersion` - currently `1`.
 * - `taskPath` - the task this file was recorded for, so that a file lifted out of its build
 *   directory still says where it came from.
 * - `duration` - milliseconds; the whole run at the top level, Gradle's own timing on a suite, summed
 *   over the rounds a retry ran that suite in. **Optional** on both, always present on a test.
 *   TeamCity has nowhere to put a suite's duration, so these are for other consumers, such as
 *   distributing tests by how long they take.
 * - `suites` and `tests` - always written, even when empty.
 * - suite `name` - as Gradle reports it, minus the suites Gradle inserts itself (see
 *   [isGradleInsertedSuiteName]), and under the bare class where a task prefixes a class suite with
 *   its own name (see [recordedSuiteName]). The suite standing for a test's own class is **kept**:
 *   that is where per-class timings hang, and collapsing it is the replay's job.
 * - test `name` - the **method part only**, in the form TeamCity's runner sends, see
 *   [teamCityRunnerMethodName]. What TeamCity is told is `className` and `name` joined with a `.`,
 *   see [qualifying].
 * - test `className` - three states that differ: **absent** means the enclosing suite is the test's
 *   class; **present** names a class that suite does not stand for, being narrower, as for a
 *   parameterized invocation; **explicitly `null`** means the test has no class at all. An empty
 *   string means that too, Gradle's other spelling of it, and is written through as reported.
 * - test `status` - `OK`, `Failure` or `Ignored`, or a result type Gradle added since, see
 *   [statusName]. A `Failure` can appear in a file recorded by a *successful* task: passing after a
 *   retry is not a failure, see [configureTestRetries].
 * - Tests appear in **execution order**, which is significant: a retried test is recorded once per
 *   attempt, its `Failure` before its `OK`, and replaying that order is what lets TeamCity see it as
 *   flaky rather than as failed.
 *
 * ### Why JSON rather than YAML
 *
 * Test names carry `:`, `#`, quotes, apostrophes and newlines, and JSON escaping is unconditional
 * where YAML quoting depends on the character and on its position. No YAML parser ships with Gradle
 * either, while `groovy.json.JsonSlurper` does - and JSON is a strict subset of YAML 1.2, so a YAML
 * parser reads it unchanged if it is ever wanted.
 */
class TestExecutionsListener(
    private val taskName: String,
    private val taskPath: String,
    buildDir: Provider<File>,
) : TestListener {
    // Next to 'test-inventory.tsv': the two describe the same run, so collecting one finds the other.
    val executionsFile: Provider<File> =
        buildDir.map { it.resolve("test-inventory").resolve(taskName).resolve("test-executions.json") }

    private class SuiteNode(val name: String) {
        val suites = LinkedHashMap<String, SuiteNode>()
        val tests = mutableListOf<TestRecord>()

        /** What Gradle timed this suite at, summed over its rounds, or null if it reported none. */
        var durationMillis: Long? = null
    }

    private class TestRecord(
        val name: String,
        val className: String?,
        val status: String,
        val durationMillis: Long,
    )

    /** Holds the top level suites and any test reported without an enclosing suite. */
    private val root = SuiteNode(name = "")

    // One task's events are not necessarily dispatched from one thread once tests run in parallel forks.
    private val lock = Any()

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        // The runner's form, not the inventory's: TeamCity normalizes a replayed message the same
        // way it normalizes the runner's own, see [teamCityRunnerMethodName].
        val record = TestRecord(
            // The method part only; a replay joins it back to the class, see [qualifying].
            name = testDescriptor.teamCityRunnerMethodName(),
            // Kept so that a replay can collapse the suite repeating it, as TeamCity names tests.
            className = testDescriptor.className,
            status = result.statusName(),
            durationMillis = result.durationMillis,
        )

        synchronized(lock) { nodeFor(testDescriptor.enclosingSuiteNames(taskName)).tests += record }
    }

    /**
     * The node [suite] stands for, or null for a suite Gradle inserted itself and that therefore has
     * none - except the outermost one, which encloses the whole run and so is the root.
     */
    private fun nodeOf(suite: TestDescriptor): SuiteNode? = when {
        suite.parent == null -> root
        isGradleInsertedSuiteName(suite.name, taskName) -> null
        else -> nodeFor(suite.enclosingSuiteNames(taskName) + suite.recordedSuiteName(taskName))
    }

    /**
     * Whether [test] can take its class from this suite, so that the record need not repeat it: either
     * the suite stands for that very class, or the test has no class and sits outside any suite.
     */
    private fun SuiteNode.enclosesClassOf(test: TestRecord): Boolean =
        test.className == name || (test.className == null && name.isEmpty())

    /** Walks to the node at [suiteNames], creating the nodes along the way. */
    private fun nodeFor(suiteNames: List<String>): SuiteNode =
        // A suite is identified by its name within its parent, so equal names are merged.
        suiteNames.fold(root) { parent, name -> parent.suites.getOrPut(name) { SuiteNode(name) } }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        // Summed rather than assigned: a retry reruns the suite under a fresh executor suite, one of
        // the suites Gradle inserts, so every round lands on this node - and every round cost time.
        synchronized(lock) {
            nodeOf(suite)?.let { it.durationMillis = (it.durationMillis ?: 0) + result.durationMillis }
        }

        // Only the root suite finishing means the whole task is done.
        if (suite.parent != null) return

        val document = synchronized(lock) { render() }

        val outputFile = executionsFile.get()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(document, Charsets.UTF_8)
    }

    private fun render(): String = buildString {
        append("{\n")
        append("  \"formatVersion\": 1,\n")
        // The full path, so that a file lifted out of its build directory still says which task.
        append("  \"taskPath\": ").appendJsonString(taskPath).append(",\n")
        // The root suite's own timing: how long the task's whole test run took.
        root.durationMillis?.let { append("  \"duration\": ").append(it).append(",\n") }
        appendNodeMembers(root, indent = "  ")
        append("}\n")
    }

    /**
     * Appends the `suites` and `tests` members of [node]. Both are always written, even when empty, so
     * that a consumer never has to distinguish an absent member from an empty one.
     */
    private fun StringBuilder.appendNodeMembers(node: SuiteNode, indent: String) {
        val suites = node.suites.values.toList()
        append(indent).append("\"suites\": ")
        if (suites.isEmpty()) {
            append("[]")
        } else {
            append("[\n")
            val itemIndent = "$indent  "
            suites.forEachIndexed { index, suite ->
                append(itemIndent).append("{\n")
                append(itemIndent).append("  \"name\": ").appendJsonString(suite.name).append(",\n")
                suite.durationMillis?.let {
                    append(itemIndent).append("  \"duration\": ").append(it).append(",\n")
                }
                appendNodeMembers(suite, "$itemIndent  ")
                append(itemIndent).append("}")
                if (index != suites.lastIndex) append(',')
                append('\n')
            }
            append(indent).append(']')
        }
        append(",\n")

        append(indent).append("\"tests\": ")
        if (node.tests.isEmpty()) {
            append("[]")
        } else {
            append("[\n")
            node.tests.forEachIndexed { index, test ->
                append(indent).append("  { \"name\": ").appendJsonString(test.name)
                // Written only where it is not the enclosing suite's, so an absent 'className' means
                // "the suite I am in" and an explicit null means "no class at all" - replayed differently.
                if (!node.enclosesClassOf(test)) {
                    append(", \"className\": ")
                    if (test.className == null) append("null") else appendJsonString(test.className)
                }
                append(", \"status\": ").appendJsonString(test.status)
                append(", \"duration\": ").append(test.durationMillis)
                append(" }")
                if (index != node.tests.lastIndex) append(',')
                append('\n')
            }
            append(indent).append(']')
        }
        append('\n')
    }

    /** Appends [value] as a JSON string literal, so that no test name can break out of it. */
    private fun StringBuilder.appendJsonString(value: String): StringBuilder {
        append('"')
        for (char in value) {
            when {
                char == '\\' -> append("\\\\")
                char == '"' -> append("\\\"")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char.code < 0x20 -> append("\\u").append("%04x".format(char.code))
                else -> append(char)
            }
        }
        append('"')
        return this
    }
}
