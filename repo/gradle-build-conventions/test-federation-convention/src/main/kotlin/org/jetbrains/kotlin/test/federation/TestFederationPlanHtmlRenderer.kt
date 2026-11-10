/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import kotlinx.html.*
import kotlinx.html.stream.createHTML

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
                renderLegend()
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
            entry.isChanged -> strong { +"$domainName*" }
            else -> +domainName
        }
    }

    private fun TD.renderTestSubsets(entry: TestFederationPlan.PlanEntry) {
        entry.testSubsets.forEachIndexed { index, subset ->
            if (index > 0) +" + "
            +subset.toEmoji()
        }
    }

    private fun TD.renderReason(entry: TestFederationPlan.PlanEntry) {
        +entry.reason
    }

    private fun BODY.renderLegend() {
        p { +"Legend:" }
        table {
            tr {
                th { +"Symbol" }
                th { +"Meaning" }
            }
            tr { td { strong { +"Domain*" } }; td { +"The domain is changed" } }
            tr { td { +ALL_TESTS_EMOJI }; td { +"AllTests" } }
            tr { td { +SMOKE_TESTS_EMOJI }; td { +"SmokeTests" } }
            tr { td { +"$CONTRACT_TESTS_EMOJI (Domain)" }; td { +"ContractTestsForDomain" } }
        }
    }
}
