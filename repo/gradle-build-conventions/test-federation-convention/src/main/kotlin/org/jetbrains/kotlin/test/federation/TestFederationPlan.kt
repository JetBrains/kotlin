/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.jetbrains.kotlin.test.federation.TestFederationPlanAsciiRenderer
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
 * The CI consumes this plan to schedule which TeamCity configurations should run which test subsets.
 */
internal class TestFederationPlan private constructor(
    val entries: List<PlanEntry>,
) {
    private val htmlRenderer = TestFederationPlanHtmlRenderer(this)
    private val asciiRenderer = TestFederationPlanAsciiRenderer(this)

    companion object {
        fun resolve(
            isDefaultBranch: Boolean,
            changedDomains: Set<Domain>,
            affectedDomains: Set<Domain>,
        ): TestFederationPlan {
            val rows = allDomainInfos.map { domainInfo ->
                val isChanged = domainInfo.domain in changedDomains
                val isAffected = domainInfo.domain in affectedDomains

                val changedDependencies = domainInfo.mustRunAllTestsOnChangesIn
                    .filter { it.domain in changedDomains }

                PlanEntry(
                    domain = domainInfo.domain,
                    testSubsets = when {
                        isAffected -> listOf(AllTests)
                        else -> listOf(SmokeTests) + changedDomains.sorted().map(::contractTestsSubsetOf)
                    },
                    isChanged = isChanged,
                    reason = when {
                        isDefaultBranch && changedDomains.containsAll(Domain.entries) -> "running on the default branch"
                        isChanged -> "this domain is changed"
                        changedDependencies.isNotEmpty() -> "mustRunAllTestsOnChangesIn: ${changedDependencies.joinToString(", ")}"
                        else -> ""
                    },
                )
            }
            return TestFederationPlan(rows)
        }
    }

    /**
     * Renders the plan into a `.properties` file to be consumed by TeamCity
     *
     * Keys, for every domain:
     * - `<domain>.testSubsets` - test subsets (comma-separated)
     * - `<domain>.changed` - `true` if the domain is changed / `false` otherwise
     */
    fun renderProperties(): String = entries.joinToString("\n", postfix = "\n") {
        buildString {
            appendLine("${it.domain.name}.testSubsets=${it.testSubsets.toArgumentString()}")
            append("${it.domain.name}.changed=${it.isChanged}")
        }
    }

    /**
     * Renders the plan as a standalone HTML page to be shown as a report tab in TeamCity
     */
    fun renderHtml(): String = htmlRenderer.renderHtml()

    /**
     * Renders the plan as a table for the build log
     */
    fun renderAscii(): String = asciiRenderer.renderAscii()

    internal class PlanEntry(
        val domain: Domain,
        val testSubsets: List<TestSubset>,
        val isChanged: Boolean,
        val reason: String,
    )
}

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
