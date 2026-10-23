/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.federation

import org.jetbrains.kotlin.testFederation.TestFederationPlan
import kotlin.collections.maxOf
import kotlin.collections.plus

internal class TestFederationPlanAsciiRenderer(plan: TestFederationPlan) {

    private val header = listOf("Domain", "Test subsets", "Reason")
    private val rows = plan.entries.map { row ->
        val domainName = row.domain.name + if (row.isChanged) "*" else ""
        val testSubsets = row.testSubsets.joinToString(", ") { it.name }
        val reason = row.reason
        listOf(domainName, testSubsets, reason)
    }
    private val allRows = listOf(header) + rows
    private val widths = header.indices.map { column -> allRows.maxOf { row -> row[column].length } }

    fun renderAscii(): String = buildList {
        add(line("┌", "┬", "┐"))
        add(row(header))
        add(line("├", "┼", "┤"))
        rows.forEach { add(row(it)) }
        add(line("└", "┴", "┘"))
    }.joinToString("\n")

    private fun line(left: String, middle: String, right: String) =
        widths.joinToString(middle, left, right) { "─".repeat(it + 2) }

    private fun row(cells: List<String>) =
        cells.indices.joinToString(" │ ", "│ ", " │") { cells[it].padEnd(widths[it]) }
}
