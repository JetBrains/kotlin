package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.checkers.ClassAnnotationsChecker
import org.jetbrains.kotlin.abicmp.checkers.FieldsListChecker
import org.jetbrains.kotlin.abicmp.checkers.InnerClassesListChecker
import org.jetbrains.kotlin.abicmp.checkers.MethodsListChecker
import org.jetbrains.kotlin.abicmp.defects.*
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

val METADATA_DIFF_D = DefectType("class.metadataDiff", "Difference in metadata", VALUE1_A, VALUE2_A)

class ClassReport(
    private val location: Location.Class,
    val classInternalName: String,
    val header1: String,
    val header2: String,
    private val defectReport: DefectReport,
) : ComparisonReport {

    private val infoParagraphs = ArrayList<String>()

    private val metadataDiff = ArrayList<TextDiffEntry>()
    private val propertyDiffs = ArrayList<NamedDiffEntry>()
    private val annotationDiffs = ArrayList<NamedDiffEntry>()
    private val innerClassesDiffs = ArrayList<DiffEntry>()
    private val methodListDiffs = ArrayList<DiffEntry>()
    private val fieldListDiffs = ArrayList<DiffEntry>()

    private val methodReports = ArrayList<MethodReport>()
    private val fieldReports = ArrayList<FieldReport>()

    override fun isEmpty(): Boolean =
                metadataDiff.isEmpty() &&
                propertyDiffs.isEmpty() &&
                annotationDiffs.isEmpty() &&
                innerClassesDiffs.isEmpty() &&
                methodListDiffs.isEmpty() &&
                getFilteredMethodReports().isEmpty() &&
                fieldListDiffs.isEmpty() &&
                getFilteredFieldReports().isEmpty()

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

    fun addMetadataDiff(diff: TextDiffEntry) {
        metadataDiff.add(diff)
        METADATA_DIFF_D.report(VALUE1_A to diff.lines1.joinToString("\n"), VALUE2_A to diff.lines2.joinToString("\n"))
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

    private fun reportMissing(diff: ListEntryDiff, missing1: DefectType, missing2: DefectType, attr: DefectAttribute) {
        when {
            diff.value1 == null && diff.value2 != null ->
                missing1.report(attr to diff.value2)
            diff.value1 != null && diff.value2 == null ->
                missing2.report(attr to diff.value1)
        }
    }

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
            if (metadataDiff.isNotEmpty()) {
                node("@kotlin.Metadata")
                for (diff in metadataDiff) {
                    node(header1) {
                        node(diff.lines1.joinToString("\n"))
                    }
                    node(header2) {
                        node(diff.lines2.joinToString("\n"))
                    }
                }
            }

            appendNamedDiffEntries(header1, header2, propertyDiffs, "Property")
            appendNamedDiffEntries(header1, header2, annotationDiffs, "Annotation")


            appendDiffEntries(header1, header2, innerClassesDiffs)
            appendDiffEntries(header1, header2, methodListDiffs)

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

        if (metadataDiff.isNotEmpty()) {
            output.tag("h3", "@kotlin.Metadata")
            output.table {
                output.tableHeader(header1, header2)
                for (diff in metadataDiff) {
                    output.tableData(
                        diff.lines1.toDiffTableData(),
                        diff.lines2.toDiffTableData()
                    )
                }
            }
            output.println("&nbsp;")
        }

        output.propertyDiffTable(header1, header2, propertyDiffs)

        output.annotationDiffTable(header1, header2, annotationDiffs)

        output.listDiff(header1, header2, innerClassesDiffs)

        output.listDiff(header1, header2, methodListDiffs)

        for (mr in getFilteredMethodReports()) {
            mr.writeAsHtml(output)
        }

        output.listDiff(header1, header2, fieldListDiffs)

        for (fr in getFilteredFieldReports()) {
            fr.writeAsHtml(output)
        }
    }

    private fun List<String>.toDiffTableData(): String =
        buildString {
            for (line in this@toDiffTableData) {
                append("<code>${line.escapeHtml()}</code>")
                append("<br>")
            }
        }
}