/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import java.io.PrintWriter

class ModuleMetadataReport(header1: String, header2: String) : MetadataPropertyReport("MODULE METADATA", header1, header2) {

    private val packagePartsReports = ArrayList<MetadataPropertyReport>()

    override fun isEmpty() = propertyDiffs.isEmpty() && getFilteredPackagePartsReports().isEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return
        super.writeAsHtml(output)
        packagePartsReports.forEach { it.writeAsHtml(output) }
    }

    fun TextTreeBuilderContext.appendModuleMetadataReport() {
        node("MODULE METADATA") {
            appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")

            for (report in getFilteredPackagePartsReports()) {
                with(report) { appendReport() }
            }
        }
    }

    fun packagePartsReport(id: String) = MetadataPropertyReport(id, header1, header2).also { packagePartsReports.add(it) }

    private fun getFilteredPackagePartsReports() = packagePartsReports.filter { !it.isEmpty() }.sortedBy { it.id }

}