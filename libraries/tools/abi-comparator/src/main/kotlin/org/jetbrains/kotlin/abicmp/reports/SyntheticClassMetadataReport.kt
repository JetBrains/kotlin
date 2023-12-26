/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.PrintWriter

class SyntheticClassMetadataReport(private val classInternalName: String, val header1: String, val header2: String) : ComparisonReport {

    private val propertyDiffs = ArrayList<NamedDiffEntry>()

    private var functionReport: MetadataPropertyReport? = null

    override fun isEmpty() = propertyDiffs.isEmpty() && functionReport.isNullOrEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return
        output.tag("h2", "SYNTHETIC CLASS METADATA $classInternalName")
        functionReport?.run { writeAsHtml(output) }
    }

    fun functionReport() = MetadataPropertyReport("function", header1, header2).also { functionReport = it }

    fun addPropertyDiff(diff: NamedDiffEntry) {
        propertyDiffs.add(diff)
    }

    fun TextTreeBuilderContext.appendSyntheticClassMetadataReport() {
        if (isNotEmpty()) {
            node("SYNTHETIC CLASS METADATA") {
                appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")
            }
        }
    }
}