/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.tag
import java.io.PrintWriter

class MultiFileClassFacadeMetadataReport(private val classInternalName: String, val header1: String, val header2: String) :
    ComparisonReport {

    private val membersDiffList = ArrayList<DiffEntry>()

    override fun isEmpty() = membersDiffList.isEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        if (isEmpty()) return
        output.tag("h2", "MULTIFILE CLASS FACADE METADATA $classInternalName")
        output.listDiff(header1, header2, membersDiffList)
    }

    fun addMembersListDiffs(diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            membersDiffList.add(diff.toDiffEntry())
        }
    }

    fun TextTreeBuilderContext.appendMultiFileClassFacadeReport() {
        if (isNotEmpty()) {
            node("MULTIFILE FILE FACADE METADATA") {
                appendDiffEntries(header1, header2, membersDiffList)
            }
        }
    }
}