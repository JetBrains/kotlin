/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.PrintWriter

class PackageMetadataReport(private val classInternalName: String, val header1: String, val header2: String) : ComparisonReport {

    private val membersDiffList = ArrayList<DiffEntry>()

    private val functionReports = ArrayList<MetadataPropertyReport>()
    private val propertyReports = ArrayList<MetadataPropertyReport>()
    private val typeAliasReports = ArrayList<MetadataPropertyReport>()
    private val localDelegatedPropertyReport = ArrayList<MetadataPropertyReport>()


    override fun isEmpty() =
        membersDiffList.isEmpty()
                && functionReports.areAllEmpty()
                && propertyReports.areAllEmpty()
                && typeAliasReports.areAllEmpty()
                && localDelegatedPropertyReport.areAllEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return

        output.tag("h2", "PACKAGE METADATA $classInternalName")
        output.listDiff(header1, header2, membersDiffList)

        for (report in listOf(
            functionReports,
            propertyReports,
            typeAliasReports,
            localDelegatedPropertyReport
        ).flatten()) {
            report.writeAsHtml(output)
        }
    }

    fun addMembersListDiffs(diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            membersDiffList.add(diff.toDiffEntry())
        }
    }

    fun functionReport(id: String) = MetadataPropertyReport("FUNCTION $id", header1, header2).also { functionReports.add(it) }

    fun propertyReport(id: String) = MetadataPropertyReport("PROPERTY $id", header1, header2).also { propertyReports.add(it) }

    fun typeAliasReport(id: String) = MetadataPropertyReport("TYPE ALIAS $id", header1, header2).also { typeAliasReports.add(it) }

    fun localDelegatedPropertyReport(id: String) =
        MetadataPropertyReport("LOCAL DELEGATED PROPERTY $id", header1, header2).also { localDelegatedPropertyReport.add(it) }

    fun TextTreeBuilderContext.appendPackageMetadataReport() {
        if (isNotEmpty()) {
            node("PACKAGE METADATA") {
                appendDiffEntries(header1, header2, membersDiffList)

                for (report in listOf(
                    functionReports,
                    propertyReports,
                    typeAliasReports,
                    localDelegatedPropertyReport
                ).flatten()) {
                    with(report) { appendReport() }
                }
            }
        }
    }
}