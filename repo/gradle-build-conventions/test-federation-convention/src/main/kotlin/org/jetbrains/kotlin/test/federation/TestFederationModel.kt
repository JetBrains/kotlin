/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.jetbrains.kotlin.test.federation.TestFederationModelAsciiRenderer
import org.jetbrains.kotlin.testFederation.TestSubset.AllTests
import org.jetbrains.kotlin.testFederation.TestSubset.SmokeTests

/**
 * Describes which tests every [Domain] runs for the current change.
 *
 * Example:
 * ```
 * ┌────────────────┬───────────────────────────────────────────┐
 * │ Domain         │ Test subsets                              │
 * ├────────────────┼───────────────────────────────────────────┤
 * │ CommonBackend* │ AllTests                                  │
 * │ Jvm            │ AllTests                                  │
 * │ Gradle         │ SmokeTests, ContractTestsForCommonBackend │
 * └────────────────┴───────────────────────────────────────────┘
 * ```
 *
 * In the example above:
 * - CommonBackend is changed (marked with `*`), so it runs AllTests
 * - Jvm is not changed, but it declares `mustRunAllTestsOnChangesIn: CommonBackend`, so it runs AllTests
 * - Gradle is neither changed, nor it declares `mustRunAllTestsOnChangesIn`, so it runs SmokeTests + ContractTestsForCommonBackend
 *
 * The CI consumes this model to resolve which TeamCity configurations should run which test subsets.
 */
internal class TestFederationModel private constructor(
    val rows: List<TestFederationModelRow>,
) {
    private val htmlRenderer = TestFederationModelHtmlRenderer(this)
    private val asciiRenderer = TestFederationModelAsciiRenderer(this)

    companion object {
        fun compute(
            isDefaultBranch: Boolean,
            changedDomains: Set<Domain>,
            affectedDomains: Set<Domain>,
        ): TestFederationModel {
            val rows = Domain.entries.map { domain ->
                val isChanged = domain in changedDomains
                val isAffected = domain in affectedDomains

                val domainInfo = allDomainInfos.first { it.domain == domain }
                val dependencies = domainInfo.mustRunAllTestsOnChangesIn.map(DomainInfo::domain)
                val changedDependencies = dependencies.filter { it in changedDomains }

                TestFederationModelRow(
                    domain = domain,
                    testSubsets = when {
                        isAffected -> listOf(AllTests)
                        else -> listOf(SmokeTests) + changedDomains.sorted().map(::contractTestsSubsetOf)
                    },
                    isChanged = isChanged,
                    reason = when {
                        isDefaultBranch && changedDomains.containsAll(Domain.entries) -> "test federation is disabled"
                        isChanged -> "this domain is changed"
                        changedDependencies.isNotEmpty() -> "mustRunAllTestsOnChangesIn: ${changedDependencies.joinToString(", ")}"
                        else -> ""
                    },
                )
            }
            return TestFederationModel(rows)
        }
    }

    /**
     * Renders the table into TSV to be consumed by TeamCity
     *
     * Columns:
     * - domain name
     * - test subsets (comma-separated)
     * - `changed` if the domain is changed / `-` otherwise
     */
    fun renderTsv(): String = rows.joinToString("\n", postfix = "\n") {
        listOf(
            it.domain.name,
            it.testSubsets.joinToString(",") { subset -> subset.name },
            if (it.isChanged) "changed" else "-",
        ).joinToString("\t")
    }

    /**
     * Renders the model as a standalone HTML page to be shown as a report tab in TeamCity
     */
    fun renderHtml(): String = htmlRenderer.renderHtml()

    /**
     * Renders the model as a table for the build log
     */
    fun renderAscii(): String = asciiRenderer.renderAscii()
}

internal class TestFederationModelRow(
    val domain: Domain,
    val testSubsets: List<TestSubset>,
    val isChanged: Boolean,
    val reason: String,
)

internal const val ALL_TESTS_EMOJI = "🛡️"
internal const val SMOKE_TESTS_EMOJI = "🔥"
internal const val CONTRACT_TESTS_EMOJI = "✍️"

internal fun TestSubset.toEmoji(): String = when (this) {
    AllTests -> ALL_TESTS_EMOJI
    SmokeTests -> SMOKE_TESTS_EMOJI
    else -> {
        val domain = Domain.entries.first { contractTestsSubsetOf(it) == this }
        "$CONTRACT_TESTS_EMOJI (${domain.name})"
    }
}
