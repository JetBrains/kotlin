/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.defects.*
import org.jetbrains.kotlin.abicmp.tag
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

val MISSING_CLASS1_D = DefectType("jar.missingClass1", "Missing class in #1", CLASS_A, JAR_FILE1_A)
val MISSING_CLASS2_D = DefectType("jar.missingClass2", "Missing class in #2", CLASS_A, JAR_FILE2_A)

class JarReport(
    private val header: String,
    private val header1: String,
    private val header2: String,
    private val jarFileName1: String,
    private val jarFileName2: String,
) : ComparisonReport {
    private val infoParagraphs = ArrayList<String>()

    private val classReports = ArrayList<ClassReport>()

    private val missingClassNames1 = HashSet<String>()
    private val missingClassNames2 = HashSet<String>()

    val defectReport = DefectReport()

    fun classLocation(classInternalName: String) =
        Location.Class(jarFileName1, classInternalName)

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

    fun classReport(classInternalName: String) =
        ClassReport(classLocation(classInternalName), classInternalName, header1, header2, defectReport)
            .also { classReports.add(it) }

    private fun getFilteredClassReports(): List<ClassReport> =
        classReports.filter { !it.isEmpty() }.sortedBy { it.classInternalName }

    private val jar1Location = Location.JarFile(jarFileName1)
    private val jar2Location = Location.JarFile(jarFileName2)

    fun addMissingClassName1(classInternalName: String) {
        missingClassNames1.add(classInternalName)
        defectReport.report(
            MISSING_CLASS1_D,
            jar1Location,
            CLASS_A to classInternalName,
            JAR_FILE1_A to jarFileName1
        )
    }

    fun addMissingClassName2(classInternalName: String) {
        missingClassNames2.add(classInternalName)
        defectReport.report(
            MISSING_CLASS2_D,
            jar2Location,
            CLASS_A to classInternalName,
            JAR_FILE2_A to jarFileName2
        )
    }

    override fun isEmpty(): Boolean =
        defectReport.isEmpty()

    override fun writeAsHtml(output: PrintWriter) {
        output.tag("h1", header)

        for (info in infoParagraphs) {
            output.tag("p", info)
        }

        for (classReport in getFilteredClassReports()) {
            classReport.writeAsHtml(output)
        }

        writeMissingClasses(output, jarFileName1, missingClassNames1)
        writeMissingClasses(output, jarFileName2, missingClassNames2)
    }

    private fun writeMissingClasses(output: PrintWriter, name: String, missing: Collection<String>) {
        if (missing.isNotEmpty()) {
            output.tag("p", "Classes missing in $name: <b>${missing.size}</b>")
            output.tag("ul") {
                for (className in missing) {
                    output.tag("li", className)
                }
            }
        }
    }
}

