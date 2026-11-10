/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.kotlin.testFederation.TestSubset.AllTests
import org.jetbrains.kotlin.testFederation.TestSubset.SmokeTests

internal class TestFederationPlanHtmlRenderer(
    private val plan: TestFederationPlan
) {
    companion object {
        private const val STYLE_CSS_FILE = ".test-federation.style.css"
        private const val DARK_THEME_JS_FILE = ".test-federation.dark-theme.js"
    }

    fun renderHtml(): String =
        createHTML(prettyPrint = true).html {
            lang = "en"
            head {
                meta(charset = "utf-8")
                title("Test Federation")
                link(rel = "stylesheet", href = STYLE_CSS_FILE)
                script(src = DARK_THEME_JS_FILE) {}
            }
            body {
                renderPlan()
            }
        }

    private fun BODY.renderPlan() {
        table {
            tr {
                th { +"Domain" }
                th { +"Test subsets" }
                th { +"Reason" }
            }
            plan.entries.forEach { entry ->
                tr {
                    td { renderDomain(entry) }
                    td { renderTestSubsets(entry) }
                    td { renderReason(entry) }
                }
            }
        }
    }

    private fun TD.renderDomain(entry: TestFederationPlan.PlanEntry) {
        val domainName = entry.domain.name
        when {
            entry.isChanged -> strong { +domainName }
            else -> +domainName
        }
    }

    private fun TD.renderTestSubsets(entry: TestFederationPlan.PlanEntry) {
        entry.testSubsets.forEachIndexed { index, subset ->
            if (index > 0) +" + "
            renderTestSubset(subset)
        }
    }

    private fun TD.renderTestSubset(subset: TestSubset) {
        when (subset) {
            AllTests -> {
                span(classes = "chip chip-all-tests") {
                    +"$ALL_TESTS_EMOJI all tests"
                }
            }
            SmokeTests -> {
                span(classes = "chip chip-smoke-tests") {
                    dataAnnotation = "@MustRunAlways"
                    +"$SMOKE_TESTS_EMOJI smoke tests"
                }
            }
            else -> {
                val domain = Domain.entries.first { contractTestsSubsetOf(it) == subset }
                span(classes = "chip chip-contract-tests") {
                    dataAnnotation = "@MustRunOnChangesIn${domain.name}"
                    +"$CONTRACT_TESTS_EMOJI contracts for ${domain.name}"
                }
            }
        }
    }

    private fun TD.renderReason(entry: TestFederationPlan.PlanEntry) {
        val reason = entry.reason
        when {
            reason.startsWith(MUST_RUN_ALL_TESTS_REASON_PREFIX) -> code { +reason }
            else -> +reason
        }
    }
}

private var CoreAttributeGroupFacade.dataAnnotation : String
    get() = attributes["data-annotation"].orEmpty()
    set(newValue) { attributes["data-annotation"] = newValue }
