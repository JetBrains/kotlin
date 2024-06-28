/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.checkers.ClassAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.FieldsListChecker
import org.jetbrains.kotlin.abicmp.checkers.InnerClassesListChecker
import org.jetbrains.kotlin.abicmp.checkers.MethodsListChecker
import org.jetbrains.kotlin.abicmp.defects.*
import org.jetbrains.kotlin.abicmp.escapeHtml
import org.jetbrains.kotlin.abicmp.tag
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class ClassReport(
    private val location: Location.Class,
    val classInternalName: String,
    val header1: String,
    val header2: String,
    private val defectReport: DefectReport,
) : ComparisonReport {

    private val infoParagraphs = ArrayList<String>()

    private val propertyDiffs = ArrayList<NamedDiffEntry>()
    private val annotationDiffs = ArrayList<NamedDiffEntry>()
    private val innerClassesDiffs = ArrayList<DiffEntry>()
    private val methodListDiffs = ArrayList<DiffEntry>()
    private val fieldListDiffs = ArrayList<DiffEntry>()
    private val metadataDiffs = ArrayList<DiffEntry>()

    private val methodReports = ArrayList<MethodReport>()
    private val fieldReports = ArrayList<FieldReport>()

    private var classMetadataReport: ClassMetadataReport? = null
    private var fileFacadeMetadataReport: PackageMetadataReport? = null
    private var multiFileClassFacadeMetadataReport: MultiFileClassFacadeMetadataReport? = null
    private var multiFileClassPartMetadataReport: MultiFileClassPartMetadataReport? = null
    private var syntheticClassMetadataReport: SyntheticClassMetadataReport? = null

    private val ComparisonReport?.isNullOrEmpty get() = this == null || this.isEmpty()

    override fun isEmpty(): Boolean =
        metadataDiffs.isEmpty() &&
                propertyDiffs.isEmpty() &&
                annotationDiffs.isEmpty() &&
                innerClassesDiffs.isEmpty() &&
                methodListDiffs.isEmpty() &&
                getFilteredMethodReports().isEmpty() &&
                fieldListDiffs.isEmpty() &&
                getFilteredFieldReports().isEmpty()
                && classMetadataReport.isNullOrEmpty
                && fileFacadeMetadataReport.isNullOrEmpty
                && multiFileClassFacadeMetadataReport.isNullOrEmpty
                && multiFileClassPartMetadataReport.isNullOrEmpty
                && syntheticClassMetadataReport.isNullOrEmpty

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

    fun addAnnotationDiffs(checker: ClassAnnotationsChecker, diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            annotationDiffs.add(NamedDiffEntry(checker.name, diff.value1 ?: "---", diff.value2 ?: "---"))
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

    fun addInnerClassesDiffs(checker: InnerClassesListChecker, diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            innerClassesDiffs.add(diff.toDiffEntry())
            reportMissing(diff, checker.missing1Defect, checker.missing2Defect, INNER_CLASS_A)
        }
    }

    fun addMethodListDiffs(checker: MethodsListChecker, diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            methodListDiffs.add(diff.toDiffEntry())
            reportMissing(diff, checker.missing1Defect, checker.missing2Defect, METHOD_A)
        }
    }

    fun addFieldListDiffs(checker: FieldsListChecker, diffs: List<ListEntryDiff>) {
        for (diff in diffs) {
            fieldListDiffs.add(diff.toDiffEntry())
            reportMissing(diff, checker.missing1Defect, checker.missing2Defect, FIELD_A)
        }
    }

    fun addMetadataDiff(diff: ListEntryDiff) {
        metadataDiffs.add(diff.toDiffEntry())
        val missing1Defect = DefectType("class.metadata.missing1", "Missing metadata in #1", METADATA_A)
        val missing2Defect = DefectType("class.metadata.missing2", "Missing metadata in #2", METADATA_A)
        reportMissing(diff, missing1Defect, missing2Defect, METADATA_A)
    }

    private fun reportMissing(diff: ListEntryDiff, missing1: DefectType, missing2: DefectType, attr: DefectAttribute) {
        when {
            diff.value1 == null && diff.value2 != null ->
                missing1.report(attr to diff.value2)
            diff.value1 != null && diff.value2 == null ->
                missing2.report(attr to diff.value1)
        }
    }

    fun classMetadataReport() = ClassMetadataReport(classInternalName, header1, header2).also { this.classMetadataReport = it }

    fun fileFacadeMetadataReport() = PackageMetadataReport(classInternalName, header1, header2).also { this.fileFacadeMetadataReport = it }

    fun multiFileClassFacadeMetadataReport() =
        MultiFileClassFacadeMetadataReport(classInternalName, header1, header2).also { this.multiFileClassFacadeMetadataReport = it }

    fun multiFileClassPartMetadataReport() =
        MultiFileClassPartMetadataReport(classInternalName, header1, header2).also { this.multiFileClassPartMetadataReport = it }

    fun syntheticMetadataReport() =
        SyntheticClassMetadataReport(classInternalName, header1, header2).also { this.syntheticClassMetadataReport = it }

    fun methodReport(methodId: String): MethodReport =
        MethodReport(location.method(methodId), methodId, header1, header2, defectReport)
            .also { methodReports.add(it) }

    fun fieldReport(fieldId: String): FieldReport =
        FieldReport(location.field(fieldId), fieldId, header1, header2, defectReport)
            .also { fieldReports.add(it) }

    private fun getFilteredMethodReports() =
        methodReports.filter { !it.isEmpty() }.sortedBy { it.methodId }

    private fun getFilteredFieldReports() =
        fieldReports.filter { !it.isEmpty() }.sortedBy { it.fieldId }

    fun TextTreeBuilderContext.appendClassReport() {
        node("CLASS $classInternalName") {
            classMetadataReport?.run { appendClassMetadataReport() }
            fileFacadeMetadataReport?.run { appendPackageMetadataReport() }
            multiFileClassFacadeMetadataReport?.run { appendMultiFileClassFacadeReport() }
            multiFileClassPartMetadataReport?.run { appendMultiFileClassPartReport() }
            syntheticClassMetadataReport?.run { appendSyntheticClassMetadataReport() }

            appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")
            appendNamedDiffEntries(header1, header2, annotationDiffs, "Annotation")


            appendDiffEntries(header1, header2, innerClassesDiffs)
            appendDiffEntries(header1, header2, methodListDiffs)
            appendDiffEntries(header1, header2, metadataDiffs)

            for (mr in getFilteredMethodReports()) {
                with(mr) { appendMethodReport() }
            }

            appendDiffEntries(header1, header2, fieldListDiffs)

            for (fr in getFilteredFieldReports()) {
                with(fr) { appendFieldReport() }
            }
        }
    }

    override fun writeAsHtml(output: PrintWriter) {
        output.tag("h1", "CLASS " + classInternalName.escapeHtml())

        for (info in infoParagraphs) {
            output.tag("p", info)
        }

        output.propertyDiffTable(header1, header2, propertyDiffs)

        output.annotationDiffTable(header1, header2, annotationDiffs)

        output.listDiff(header1, header2, innerClassesDiffs)

        output.listDiff(header1, header2, methodListDiffs)

        output.listDiff(header1, header2, metadataDiffs)

        classMetadataReport?.run { writeAsHtml(output) }
        fileFacadeMetadataReport?.run { writeAsHtml(output) }
        multiFileClassFacadeMetadataReport?.run { writeAsHtml(output) }
        multiFileClassPartMetadataReport?.run { writeAsHtml(output) }
        syntheticClassMetadataReport?.run { writeAsHtml(output) }

        for (mr in getFilteredMethodReports()) {
            mr.writeAsHtml(output)
        }

        output.listDiff(header1, header2, fieldListDiffs)

        for (fr in getFilteredFieldReports()) {
            fr.writeAsHtml(output)
        }
    }

}