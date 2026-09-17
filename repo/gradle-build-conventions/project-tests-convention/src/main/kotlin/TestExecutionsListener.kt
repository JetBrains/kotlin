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
        isSyntheticSuiteName(suite.name, taskName, suite.className) -> null
        else -> nodeFor(suite.enclosingSuiteNames(taskName) + suite.name)
    }

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
                test.className?.let { append(", \"className\": ").appendJsonString(it) }
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
