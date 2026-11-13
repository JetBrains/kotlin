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
        private const val THEME_SWITCHER_JS_FILE = ".test-federation.theme-switcher.js"
        private const val CHIP_FOLDING_JS_FILE = ".test-federation.chip-folding.js"
        private const val FILTERING_SVG_FILE = ".test-federation.filtering-hierarchy.svg"
    }

    fun renderHtml(): String =
        createHTML(prettyPrint = true).html {
            lang = "en"
            head {
                meta(charset = "utf-8")
                title("Test Federation")
                link(rel = "stylesheet", href = STYLE_CSS_FILE)
                script(src = THEME_SWITCHER_JS_FILE) {}
                script(src = CHIP_FOLDING_JS_FILE) {}
            }
            body {
                renderThemeSwitcher()
                renderPlan()
                renderSubsetsExplanation()
                renderPrefixesExplanation()
            }
        }

    private fun BODY.renderThemeSwitcher() {
        div("theme-switcher") {
            button(classes = "theme-option") {
                attributes["theme"] = "auto"
                +"Sync with OS"
            }
            button(classes = "theme-option") {
                attributes["theme"] = "light"
                +"Light"
            }
            button(classes = "theme-option") {
                attributes["theme"] = "dark"
                +"Dark"
            }
        }
    }

    private fun BODY.renderPlan() {
        h2 { +"Execution plan" }
        p { +"(hover test subset to see the reason behind it)" }
        table {
            tr {
                th { +"Domain" }
                th { +"Test subsets" }
            }
            plan.entries.forEach { entry ->
                tr {
                    td { renderDomain(entry) }
                    td { renderTestSubsets(entry) }
                }
            }
        }
    }

    private fun TD.renderDomain(entry: TestFederationPlan.PlanEntry) {
        val domainName = entry.domain.name
        when {
            entry.isChanged -> {
                strong { +domainName }
                +" ✏️"
            }
            else -> +domainName
        }
    }

    private fun TD.renderTestSubsets(entry: TestFederationPlan.PlanEntry) {
        entry.testSubsets.forEachIndexed { index, subset ->
            if (index > 0) +" + "
            renderTestSubset(entry, subset)
        }
    }

    private fun TD.renderTestSubset(entry: TestFederationPlan.PlanEntry, subset: TestSubset) {
        when (subset) {
            AllTests -> {
                span(classes = "chip chip-all-tests") {
                    dataAnnotation = entry.reasonPerSubset.getValue(subset)
                    +ALL_TESTS_EMOJI
                    span("chip-fold") { +" all tests" }
                }
            }
            SmokeTests -> {
                span(classes = "chip chip-smoke-tests") {
                    dataAnnotation = entry.reasonPerSubset.getValue(subset)
                    +SMOKE_TESTS_EMOJI
                    span("chip-fold") { +" smoke tests" }
                }
            }
            else -> {
                val domain = Domain.entries.first { contractTestsSubsetOf(it) == subset }
                span(classes = "chip chip-contract-tests") {
                    dataAnnotation = entry.reasonPerSubset.getValue(subset)
                    +CONTRACT_TESTS_EMOJI
                    span("chip-fold") { +" contracts for" }
                    +" ${domain.name}"
                }
            }
        }
    }

    private fun BODY.renderSubsetsExplanation() {
        details {
            summary { h2 { +"What are the test subsets?" } }
            p { +"Test subset is a concept introduced by Test Federation. It's basically a subset of tests selected by a given test task." }
            p { +"You may think of it as of the 3rd level of the filtering hierarchy:" }
            img(alt = "How Test Federation filters tests", src = "$FILTERING_SVG_FILE#light", classes = "diagram-light")
            img(alt = "How Test Federation filters tests", src = "$FILTERING_SVG_FILE#dark", classes = "diagram-dark")
        }
    }

    private fun BODY.renderPrefixesExplanation() {
        details {
            summary { h2 { +"What does it mean if a build name has emoji prefix?" } }
            p { +"Each emoji represents one test subset. If the emoji is in the prefix, it means the build can run this subset." }
            p { +"If there are no subset emojis, it means the build can run any combination of test subsets, so it's fully dynamic." }
            p { +"If there is exactly one subset emoji, it means the build always runs the same tests." }
            p { +"(one exception to the above rule is the ✍️ emoji which can run contracts against dynamically selected domains)" }
            p { +"Examples:" }
            table {
                tr {
                    th { +"Build name" }
                    th { +"Meaning" }
                }
                tr { td { +"🛡 Some build" }; td { +"always runs all tests (static)" } }
                tr { td { +"🔥️ Some build" }; td { +"always runs smoke tests (static)" } }
                tr { td { +"✍️️ Some build" }; td { +"always runs contract tests (dynamic)" } }
                tr { td { +"🔥✍️️ Some build" }; td { +"can run any combination of smokes and contracts (dynamic)" } }
                tr { td { +"Some build" }; td { +"can run any combination of tests (dynamic)" } }
            }
        }
    }
}

private var CoreAttributeGroupFacade.dataAnnotation : String
    get() = attributes["data-annotation"].orEmpty()
    set(newValue) { attributes["data-annotation"] = newValue }
