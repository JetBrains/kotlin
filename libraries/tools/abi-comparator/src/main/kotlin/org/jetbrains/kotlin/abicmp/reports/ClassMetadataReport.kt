/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.PrintWriter

class ClassMetadataReport(
    private val classInternalName: String,
    val header1: String,
    val header2: String,
) : ComparisonReport {

    private val membersDiffList = ArrayList<DiffEntry>()

    private val propertyDiffs = ArrayList<NamedDiffEntry>()

    private val constructorReports = ArrayList<MetadataPropertyReport>()
    private val functionReports = ArrayList<MetadataPropertyReport>()
    private val propertyReports = ArrayList<MetadataPropertyReport>()
    private val typeAliasReports = ArrayList<MetadataPropertyReport>()
    private val localDelegatedPropertyReport = ArrayList<MetadataPropertyReport>()

    override fun isEmpty() =
        membersDiffList.isEmpty()
                && propertyDiffs.isEmpty()
                && propertyReports.areAllEmpty()
                && constructorReports.areAllEmpty()
                && functionReports.areAllEmpty()
                && typeAliasReports.areAllEmpty()
                && localDelegatedPropertyReport.areAllEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return

        output.tag("h2", "CLASS METADATA $classInternalName")

        output.listDiff(header1, header2, membersDiffList)
        output.propertyDiffTable(header1, header2, propertyDiffs)

        for (report in listOf(
            constructorReports,
            functionReports,
            propertyReports,
            typeAliasReports,
            localDelegatedPropertyReport
        ).flatten()) {
            report.writeAsHtml(output)
        }
    }


    fun constructorReport(id: String) = MetadataPropertyReport("CONSTRUCTOR $id", header1, header2).also { constructorReports.add(it) }

    fun functionReport(id: String) = MetadataPropertyReport("FUNCTION $id", header1, header2).also { functionReports.add(it) }

    fun propertyReport(id: String) = MetadataPropertyReport("PROPERTY $id", header1, header2).also { propertyReports.add(it) }

    fun typeAliasReport(id: String) = MetadataPropertyReport("TYPE ALIAS $id", header1, header2).also { typeAliasReports.add(it) }

    fun localDelegatedPropertyReport(id: String) =
        MetadataPropertyReport("LOCAL DELEGATED PROPERTY $id", header1, header2).also { localDelegatedPropertyReport.add(it) }

    fun addMembersListDiffs(diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            membersDiffList.add(diff.toDiffEntry())
        }
    }

    fun addPropertyDiff(diff: NamedDiffEntry) {
        propertyDiffs.add(diff)
    }

    fun TextTreeBuilderContext.appendClassMetadataReport() {
        if (isNotEmpty()) {
            node("CLASS METADATA") {
                appendDiffEntries(header1, header2, membersDiffList)

                appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")

                for (report in listOf(
                    constructorReports,
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