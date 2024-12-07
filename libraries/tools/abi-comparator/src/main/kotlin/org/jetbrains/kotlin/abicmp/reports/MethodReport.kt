/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.checkers.MethodAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.MethodParameterAnnotationsChecker
import org.jetbrains.kotlin.abicmp.defects.*
import org.jetbrains.kotlin.abicmp.escapeHtml
import org.jetbrains.kotlin.abicmp.tag
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class MethodReport(
    private val location: Location.Method,
    val methodId: String,
    val header1: String,
    val header2: String,
    private val defectReport: DefectReport,
) : ComparisonReport {
    private val infoParagraphs = ArrayList<String>()

    private val propertyDiffs = ArrayList<NamedDiffEntry>()
    private val annotationDiffs = ArrayList<NamedDiffEntry>()

    fun addInfo(info: String) {
        infoParagraphs.add(info)
    }

    inline fun info(fm: PrintWriter.() -> Unit) {
        val bytes = ByteArrayOutputStream()
        val ps = PrintWriter(bytes)
        ps.fm()
        ps.close()
        addInfo(String(bytes.toByteArray()))
    }

    private fun DefectType.report(vararg attributes: Pair<DefectAttribute, String>) {
        defectReport.report(this, location, *attributes)
    }

    fun addPropertyDiff(defectType: DefectType, diff: NamedDiffEntry) {
        propertyDiffs.add(diff)
        defectType.report(VALUE1_A to diff.value1, VALUE2_A to diff.value2)
    }

    fun addAnnotationDiffs(checker: MethodAnnotationsChecker, diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            annotationDiffs.add(diff.toNamedDiffEntry(checker.name))
            when {
                diff.value1 != null && diff.value2 != null ->
                    checker.mismatchDefect.report(VALUE1_A to diff.value1, VALUE2_A to diff.value2)
                diff.value1 == null && diff.value2 != null ->
                    checker.missing1Defect.report(VALUE2_A to diff.value2)
                diff.value1 != null && diff.value2 == null ->
                    checker.missing2Defect.report(VALUE1_A to diff.value1)
            }
        }
    }

    fun addValueParameterAnnotationDiffs(checker: MethodParameterAnnotationsChecker, index: Int, diffs: List<ListEntryDiff>) {
        val diffEntryName = "${checker.name}.p$index"
        for (diff in diffs) {
            annotationDiffs.add(diff.toNamedDiffEntry(diffEntryName))
            when {
                diff.value1 != null && diff.value2 != null ->
                    checker.mismatchDefect.report(
                        VP_INDEX_A to index.toString(),
                        VALUE1_A to diff.value1,
                        VALUE2_A to diff.value2
                    )
                diff.value1 == null && diff.value2 != null ->
                    checker.missing1Defect.report(
                        METHOD_A to methodId,
                        VP_INDEX_A to index.toString(),
                        VALUE2_A to diff.value2
                    )
                diff.value1 != null && diff.value2 == null ->
                    checker.missing2Defect.report(
                        METHOD_A to methodId,
                        VP_INDEX_A to index.toString(),
                        VALUE1_A to diff.value1
                    )
            }
        }
    }

    override fun isEmpty(): Boolean = propertyDiffs.isEmpty() && annotationDiffs.isEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        output.tag("h2", "&gt; METHOD " + methodId.escapeHtml())

        for (info in infoParagraphs) {
            output.tag("p", info)
        }

        output.propertyDiffTable(header1, header2, propertyDiffs)
        output.annotationDiffTable(header1, header2, annotationDiffs)
    }

    fun TextTreeBuilderContext.appendMethodReport() {
        node("METHOD $methodId") {
            appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")
            appendNamedDiffEntries(header1, header2, annotationDiffs, "Annotation")
        }
    }
}