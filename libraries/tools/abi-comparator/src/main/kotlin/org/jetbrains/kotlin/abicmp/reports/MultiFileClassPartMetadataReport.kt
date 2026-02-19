/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.PrintWriter

class MultiFileClassPartMetadataReport(private val classInternalName: String, val header1: String, val header2: String) : ComparisonReport {

    private val propertyDiffs = ArrayList<NamedDiffEntry>()
    private var packageReport: PackageMetadataReport? = null
    override fun isEmpty() = propertyDiffs.isEmpty() && packageReport.isNullOrEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return

        output.tag("h2", "MULTIFILE CLASS PART METADATA $classInternalName")

        output.propertyDiffTable(header1, header2, propertyDiffs)

        packageReport?.run { writeAsHtml(output) }
    }

    fun packageReport() = PackageMetadataReport(classInternalName, header1, header2).also { packageReport = it }

    fun addPropertyDiff(diff: NamedDiffEntry) {
        propertyDiffs.add(diff)
    }

    fun TextTreeBuilderContext.appendMultiFileClassPartReport() {
        if (isNotEmpty()) {
            node("MULTIFILE FILE PART METADATA") {
                appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")

                packageReport?.run { appendPackageMetadataReport() }
            }
        }
    }
}