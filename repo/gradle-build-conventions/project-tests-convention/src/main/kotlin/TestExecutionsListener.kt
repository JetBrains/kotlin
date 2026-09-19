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
 * Writes the executed tests to `test-executions.json`, keeping the suite nesting intact, so that the
 * tests of a task served from the build cache can be replayed to TeamCity as `testSuiteStarted` /
 * `testSuiteFinished` and `testStarted` / `testFinished` service messages.
 *
 * This is the structured counterpart of [TestInventoryListener]: the flat `test-inventory.tsv` joins
 * the suite names and the test name with `": "`, which cannot be undone, because a test name may
 * itself contain `": "`.
 *
 * The suite names are the same as the inventory's, but the test names are **not**: they are recorded in
 * the form TeamCity's own Gradle runner reports, which the server then normalizes (see
 * [teamCityRunnerMethodName]). The inventory records the normalized form, because it is compared
 * against the names TeamCity registers; this file records the form that has to be *sent* to arrive at
 * them. For all but a handful of tests the two are identical.
 *
 * ### Format
 *
 * The file is a declared output of the test task, so the build cache restores it along with the
 * task's other outputs - which is what lets a task that never ran still report its tests.
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
 * The second suite is the shape worth reading twice: a class whose tests are not directly in it.
 * JUnit 5 reports a parameterized method as a container of its own, so the class suite holds no
 * test and the invocations sit one level further in, each naming the class it belongs to.
 *
 * - `formatVersion` - currently `1`.
 * - `taskPath` - the task this file was recorded for, so that a file lifted out of its build
 *   directory still says where it came from; the directory name alone only carries the task name.
 * - `duration` - milliseconds. At the top level the whole test run, on a suite Gradle's own timing
 *   for that suite. Both are **optional**, written only where Gradle reported one; on a test it is
 *   always present. TeamCity has nowhere to put a suite's duration - `testSuiteFinished` carries
 *   none - so these exist for other consumers, such as distributing tests by how long they take.
 * - `suites` and `tests` - always written, even when empty, so that a reader never has to tell an
 *   absent member from an empty one.
 * - suite `name` - as Gradle reports it, minus the suites Gradle inserts itself (the test run, the
 *   executor, the partitions a task is split into, the task's own name), see
 *   [isGradleInsertedSuiteName]. A task that prefixes a class suite with its own name, as Kotlin/JS
 *   and Kotlin/Wasm do, is recorded under the bare class instead, so that every task family records
 *   the same shape, see [recordedSuiteName].
 *   The suite standing for a test's own class is **kept**: that is where per-class timings hang.
 *   Collapsing it into the test name is TeamCity's convention and is applied when replaying, not
 *   when recording.
 * - test `name` - the **method part only**, in the form TeamCity's own Gradle runner sends, which is
 *   why an ordinary method keeps its `()` here and loses it once the server has registered it (see
 *   [teamCityRunnerMethodName]). The name TeamCity is told is `className` and `name` joined with a
 *   `.`, see [qualifying].
 * - test `className` - three states, and they differ:
 *     - **absent** means the enclosing suite is the test's class, which is the usual case and why
 *       `skipWhitespace()` above writes no class at all;
 *     - **present** names a class the enclosing suite does not stand for, because that suite is
 *       narrower than the class - the parameterized invocation above, whose own suite is the
 *       container rather than the class;
 *     - **explicitly `null`** means the test has no class at all, which only needs saying inside a
 *       suite, where absence would otherwise mean "the suite's class".
 * - test `status` - `OK`, `Failure` or `Ignored`, the names TeamCity's own integration uses, see
 *   [statusName]. A `Failure` can appear in a file recorded by a *successful* task: tests are
 *   retried, and passing after a retry is not a failure by default, see [configureTestRetries].
 * - Tests appear in **execution order**, which is significant for exactly that reason: a retried
 *   test is recorded once per attempt, its `Failure` before its `OK`, and replaying them in that
 *   order is what lets TeamCity see it as flaky rather than as simply failed.
 *
 * ### Why JSON rather than YAML
 *
 * The document has to round-trip test names containing `:`, `#`, quotes, apostrophes, leading and
 * trailing spaces, and newlines - the very characters that make a hand-written YAML emitter risky,
 * as whether such a scalar needs quoting, and which quoting style is legal, depends on the character
 * and on its position. JSON escaping is fully specified and unconditional: every string is double
 * quoted, so no test name can change the shape of the document.
 *
 * Tooling settles it: no YAML parser ships with Gradle (`groovy-json` does, `groovy-yaml` does not),
 * so reading YAML back would mean putting a third-party parser on the build logic classpath, while
 * this file can be read with `groovy.json.JsonSlurper` from the Gradle API alone. JSON is also a
 * strict subset of YAML 1.2, so a YAML parser reads it unchanged if it is ever consumed as YAML.
 */
class TestExecutionsListener(
    private val taskName: String,
    private val taskPath: String,
    buildDir: Provider<File>,
) : TestListener {
    // Sits in the same directory as 'test-inventory.tsv': the two describe the same test run, and
    // whatever collects one as a TeamCity artifact then finds the other next to it.
    val executionsFile: Provider<File> =
        buildDir.map { it.resolve("test-inventory").resolve(taskName).resolve("test-executions.json") }

    private class SuiteNode(val name: String) {
        val suites = LinkedHashMap<String, SuiteNode>()
        val tests = mutableListOf<TestRecord>()

        /** Gradle's own timing for this suite, or null if it never reported one. */
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

    // Gradle dispatches the test events of one task, but not necessarily from a single thread once the
    // tests run in parallel forks, so the tree is guarded rather than assumed to be touched serially.
    private val lock = Any()

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        // The name is the runner's form, not the inventory's: this file is replayed as service messages,
        // and TeamCity normalizes those the same way it normalizes the runner's own. See
        // [teamCityRunnerMethodName].
        val record = TestRecord(
            // Only the method part: the class is recorded next to it rather than repeated here, now
            // that it is a suite in its own right. A replay joins the two back, see [qualifying].
            name = testDescriptor.teamCityRunnerMethodName(),
            // Kept so that a replay can collapse the suite that merely repeats it, the way TeamCity
            // names tests, without the recorded structure having to anticipate that convention.
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
        // Suites of equal name under the same parent are merged, a suite being identified by its name
        // within its parent.
        suiteNames.fold(root) { parent, name -> parent.suites.getOrPut(name) { SuiteNode(name) } }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        // Gradle times every suite it reports, down to individual (and nested) test classes. TeamCity
        // has nowhere to put those - 'testSuiteFinished' carries no duration - but they are the input a
        // test distribution mechanism needs, so they are recorded rather than dropped.
        synchronized(lock) { nodeOf(suite)?.durationMillis = result.durationMillis }

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
        // The full task path, so that a file lifted out of its build directory still says which task
        // it came from - the directory name alone only carries the task name.
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
                // The class is written only when it is not the enclosing suite's - which, for a suite
                // standing for a class, it almost always is. An absent 'className' therefore means "the
                // suite I am in", and an explicit null means "no class at all", a distinction that
                // matters because the two are replayed differently.
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

    /**
     * Appends [value] as a JSON string literal, escaping everything JSON does not allow raw inside a
     * string, so that an arbitrary test name cannot break out of the literal.
     */
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
