/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arch.config

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore
import com.tngtech.archunit.library.freeze.ViolationStore
import java.io.StringWriter
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines
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
 * Enabled by `freeze.store` in `archunit.properties`.
 */
class DeterministicViolationStore : ViolationStore {
    private val delegate = TextFileBasedViolationStore(::violationsFileName)
    private lateinit var storedRulesFile: Path
    private var storeUpdateAllowed = true
    private lateinit var refreezeCommand: String

    override fun initialize(properties: Properties) {
        delegate.initialize(properties)
        val archUnitStore = properties.getProperty("default.path", "archunit_store")
        storedRulesFile = Path(archUnitStore).resolve("stored.rules")
        storeUpdateAllowed = properties.getProperty("default.allowStoreUpdate", "true").toBoolean()
        val taskPath = properties.getProperty("default.taskPath", "<test task>")
        refreezeCommand = "./gradlew $taskPath -ParchunitRefreeze"
    }

    override fun getViolations(rule: ArchRule): List<String> =
        delegate.getViolations(rule)

    override fun contains(rule: ArchRule): Boolean =
        delegate.contains(rule)

    override fun save(rule: ArchRule, violations: List<String>) {
        validateRule(rule)
        normalizeViolations(rule, violations)
        normalizeStoredRules()
    }

    private fun validateRule(rule: ArchRule) {
        check(storeUpdateAllowed) {
            """
            |The frozen violations of rule '${rule.description}' are out of date, because violations were fixed or the rule is new.
            | - Are the violations fixed? Great job! Update the violations and pat yourself on the back.
            | - Is the rule new? Update the stored violations dump.
            |Update the store by running `$refreezeCommand` and commit the updated store.
            """.trimMargin()
        }
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

    private fun normalizeViolations(rule: ArchRule, violations: List<String>) {
        delegate.save(
            rule,
            violations.map { violation ->
                violation.replace(Regex("""\.(java|kt):\d+"""), ".$1:0")
            }
        )
    }

    private fun normalizeStoredRules() {
        if (!storedRulesFile.exists()) return
        val lines = storedRulesFile.readLines()
        val rules = lines.filterNot { it.startsWith("#") }.sorted()
        if (rules != lines) {
            storedRulesFile.writeLines(rules)
        }
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

        /** [ruleDescription] in lowercase, with every run of other characters replaced by `-`. */
        private fun violationsFileName(ruleDescription: String): String =
            ruleDescription.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + ".txt"
    }
}
