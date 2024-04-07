/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.PrintWriter

open class MetadataPropertyReport(val id: String, val header1: String, val header2: String) : ComparisonReport {

    protected val propertyDiffs = ArrayList<NamedDiffEntry>()

    override fun isEmpty() = propertyDiffs.isEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return
        output.tag("h3", id)
        output.propertyDiffTable(header1, header2, propertyDiffs)
    }

    fun addPropertyDiff(diff: NamedDiffEntry) {
        propertyDiffs.add(diff)
    }

    fun TextTreeBuilderContext.appendReport() {
        if (!isEmpty()) {
            node(id) {
                appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")
            }
        }
    }
}