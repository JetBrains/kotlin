/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test.arch

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore
import com.tngtech.archunit.library.freeze.ViolationStore
import java.io.StringWriter
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeLines

/**
 * [TextFileBasedViolationStore] whose files only change when the frozen violations or rules change:
 *
 * - Line numbers in violations are replaced with `0`.
 *   ArchUnit ignores line numbers when matching violations, so they would only add noise to the store diffs.
 * - `stored.rules` has no comment lines.
 *   [TextFileBasedViolationStore] writes it with [Properties.store], which always adds a timestamp comment.
 * - `stored.rules` is sorted, because [Properties.store] writes the rules in hash order.
 * - Violation files are named after the rule description, instead of the generated ids
 *   [TextFileBasedViolationStore] uses by default.
 *   New rules with long descriptions are refused, and their file name must be chosen by hand in `stored.rules`.
 *
 * Frozen violations can be ignored individually: copy them into the ignore file next to the rule's violations
 * file — the same file name with `-ignore` before `.txt`, e.g. `project-root-project-usages-ignore.txt`.
 * The ignore file has the same format as the violations file, and lines starting with `#` are comments.
 * The store never creates or updates ignore files.
 *
 * Ignored violations are not reported, even if they are absent from the violations file,
 * but like frozen violations they must match actual violations:
 * a stale ignored violation fails the run and must be removed from the ignore file.
 * Refreeze writes ignored violations out of the violations file, making the ignore file their only home.
 *
 * Enabled by `freeze.store` in `archunit.properties`.
 */
class DeterministicViolationStore : ViolationStore {
    private val delegate = TextFileBasedViolationStore(::violationsFileName)
    private lateinit var storeDir: Path
    private lateinit var storedRulesFile: Path
    private var storeUpdateAllowed = true
    private lateinit var refreezeCommand: String

    override fun initialize(properties: Properties) {
        delegate.initialize(properties)
        storeDir = Path(properties.getProperty("default.path", "archunit_store"))
        storedRulesFile = storeDir.resolve("stored.rules")
        storeUpdateAllowed = properties.getProperty("default.allowStoreUpdate", "true").toBoolean()
        val taskPath = properties.getProperty("default.taskPath", "<test task>")
        refreezeCommand = "./gradlew $taskPath -ParchunitRefreeze"
    }

    /**
     * Frozen violations, then ignored violations from the rule's ignore file.
     *
     * ArchUnit matches each actual violation with the first matching line of this list:
     * an ignored line must not come before an identical frozen line, and duplicates are kept,
     * because each line matches one actual violation.
     */
    override fun getViolations(rule: ArchRule): List<String> =
        storedViolations(rule) + ignoredViolations(rule)

    /** Frozen violations from the rule's violations file, or an empty list if the file does not exist. */
    private fun storedViolations(rule: ArchRule): List<String> =
        if (violationsFile(rule).exists()) delegate.getViolations(rule) else emptyList()

    override fun contains(rule: ArchRule): Boolean =
        delegate.contains(rule)

    override fun save(rule: ArchRule, violations: List<String>) {
        if (storeUpdateAllowed) {
            // Refreeze
            checkNewRuleFileName(rule)
            val ignored = ignoredViolations(rule)
            delegate.save(
                rule,
                violations.map(::normalizeViolation).filterNot { it in ignored }
            )
            normalizeStoredRules()
        } else {
            check(delegate.contains(rule)) { violationsOutOfDateMessage(rule) }
            val ignored = ignoredViolations(rule)
            val unsolvedViolations = violations.toHashSet()
            val solvedViolations = getViolations(rule).filterNot { it in unsolvedViolations }
            val solvedFrozenViolations = solvedViolations.filter { normalizeViolation(it) !in ignored }.distinct()
            val staleIgnoredViolations = solvedViolations.filter { normalizeViolation(it) in ignored }.distinct()
            val failureMessages = buildList {
                if (solvedFrozenViolations.isNotEmpty()) add(violationsOutOfDateMessage(rule))
                if (staleIgnoredViolations.isNotEmpty()) add(staleIgnoredViolationsMessage(rule, staleIgnoredViolations))
            }
            check(failureMessages.isEmpty()) { failureMessages.joinToString("\n\n") }
        }
    }

    private fun violationsOutOfDateMessage(rule: ArchRule): String =
        """
        |The frozen violations of rule '${rule.description}' are out of date, because violations were fixed or the rule is new.
        | - Are the violations fixed? Great job! Update the violations and pat yourself on the back.
        | - Is the rule new? Update the stored violations dump.
        |Update the store by running `$refreezeCommand` and commit the updated store.
        """.trimMargin()

    private fun staleIgnoredViolationsMessage(rule: ArchRule, staleViolations: List<String>): String = buildString {
        appendLine("The ignored violations of rule '${rule.description}' are out of date, because they no longer match any actual violation:")
        staleViolations.forEach { appendLine(" - $it") }
        append("Remove them from ${ignoreFile(rule)}.")
        if (staleViolations.any { it in storedViolations(rule) }) {
            append("\nThe violations file contains them too: update the store by running `$refreezeCommand` and commit the updated store.")
        }
    }

    private fun checkNewRuleFileName(rule: ArchRule) {
        check(delegate.contains(rule) || violationsFileName(rule.description).length <= MAX_FILE_NAME_LENGTH) {
            """
            |Rule '${rule.description}' has no violations file yet, and its description is too long to be used as the file name.
            |1. Choose a shorter file name by adding this line to $storedRulesFile:
            |   ${storedRulesEntry(rule.description)}=<descriptive-name>.txt
            |
            |2. Then run `$refreezeCommand`
            """.trimMargin()
        }
    }

    /** The violation with its line number replaced with `0`. */
    private fun normalizeViolation(violation: String): String =
        violation.replace(LINE_NUMBERS, ".$1:0")

    private fun normalizeStoredRules() {
        if (!storedRulesFile.exists()) return
        val lines = storedRulesFile.readLines()
        val rules = lines.filterNot { it.startsWith("#") }.sorted()
        if (rules != lines) {
            storedRulesFile.writeLines(rules)
        }
    }

    /** Ignored violations from the rule's ignore file, or an empty list if the rule has no ignore file. */
    private fun ignoredViolations(rule: ArchRule): List<String> {
        val file = ignoreFile(rule)
        if (!file.exists()) return emptyList()
        return UNESCAPED_LINE_BREAK.split(file.readText().replace("\r\n", "\n"))
            .filter { it.isNotEmpty() }
            .map { it.trim() }
            .filterNot { it.isEmpty() || it.startsWith("#") }
            .map { it.replace("\\\n", "\n") }
            .map(::normalizeViolation)
    }

    /**
     * The rule's violations file, resolved like [TextFileBasedViolationStore] resolves it:
     * from `stored.rules`, or generated from the rule description for rules that are not stored yet.
     */
    private fun violationsFile(rule: ArchRule): Path {
        val storedViolationsFileName = if (storedRulesFile.exists()) {
            Properties().apply { storedRulesFile.inputStream().use { load(it) } }.getProperty(rule.description)
        } else {
            null
        }
        val fileName = storedViolationsFileName ?: violationsFileName(rule.description)
        return storeDir.resolve(fileName)
    }

    /** The rule's ignore file: the violations file's name with `-ignore` before `.txt`. */
    private fun ignoreFile(rule: ArchRule): Path {
        val fileName = violationsFile(rule).fileName.toString()
        return storeDir.resolve(fileName.removeSuffix(".txt") + "-ignore.txt")
    }

    /** [ruleDescription] escaped as a `stored.rules` key, the same way [Properties.store] escapes it. */
    private fun storedRulesEntry(ruleDescription: String): String {
        val output = StringWriter()
        Properties().apply { setProperty(ruleDescription, "") }.store(output, null)
        return output.toString().lines().single { it.isNotEmpty() && !it.startsWith("#") }.removeSuffix("=")
    }

    companion object {
        /** Longer file names are hard to read, and risk exceeding path length limits on Windows. */
        private const val MAX_FILE_NAME_LENGTH = 100

        /** Line numbers in violations, replaced with `0` to make the store deterministic. */
        private val LINE_NUMBERS = Regex("""\.(java|kt):\d+""")

        /** Line breaks that do not continue a violation line escaped with a backslash. */
        private val UNESCAPED_LINE_BREAK = Regex("""(?<!\\)\n""")

        /** [ruleDescription] in lowercase, with every run of other characters replaced by `-`. */
        private fun violationsFileName(ruleDescription: String): String =
            ruleDescription.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + ".txt"
    }
}
