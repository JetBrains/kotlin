/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.reports

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.defects.DefectInfo
import org.jetbrains.kotlin.abicmp.defects.Location
import java.io.File
import java.io.PrintWriter

class SummaryReport {
    private val defectsByInfo: MutableMap<DefectInfo, MutableSet<Location>> = HashMap()

    fun add(defectReport: DefectReport) {
        for (defect in defectReport.defects) {
            defectsByInfo.getOrPut(defect.info) { HashSet() }.add(defect.location)
        }
    }

    fun totalUnique() = defectsByInfo.keys.size

    fun totalDefects() = defectsByInfo.values.sumOf { it.size }

    fun writeReport(outputFile: File) {
        PrintWriter(outputFile).use { out ->
            out.tag("html") {
                out.tag("head") {
                    out.tag("style", REPORT_CSS)
                }
                out.tag("body") {
                    out.writeReportBody()
                }
            }
        }
    }

    private fun PrintWriter.writeReportBody() {
        tag("p") {
            println("Total defects: ${totalDefects().tag("b")}, unique: ${totalUnique().tag("b")}")
        }

        for (info in defectsByInfo.keys.sorted()) {
            val locations = defectsByInfo[info]!!
            writeDefectInfo(info)
            writeLocations(locations.toList().sorted())
        }
    }

    private fun PrintWriter.writeDefectInfo(info: DefectInfo) {
        tag("p") {
            tag("h2") {
                println("[${info.type.id}] ${info.type.messageText}")
            }
        }
        table {
            for ((attr, value) in info.attributes) {
                tableData(attr.htmlId, value.toHtmlString().withTag("code"))
            }
        }
        println("&nbsp;<br/>")
    }

    private fun PrintWriter.writeLocations(locations: List<Location>) {
        table {
            when (locations.first()) {
                is Location.JarFile ->
                    tableHeader("jar")
                is Location.Class ->
                    tableHeader("jar", "class")
                is Location.Method ->
                    tableHeader("jar", "class", "method")
                is Location.Field ->
                    tableHeader("jar", "class", "field")
            }
            for (location in locations) {
                when (location) {
                    is Location.JarFile ->
                        tableDataWithClass(
                                "location",
                                location.jarFileName.replace("-", NON_BREAKING_HYPHEN)
                        )
                    is Location.Class ->
                        tableDataWithClass(
                                "location",
                                location.jarFileName.replace("-", NON_BREAKING_HYPHEN),
                                location.className.withTag("code")
                        )
                    is Location.Method ->
                        tableDataWithClass(
                                "location",
                                location.jarFileName.replace("-", NON_BREAKING_HYPHEN),
                                location.className.withTag("code"),
                                location.methodName.withTag("code")
                        )
                    is Location.Field ->
                        tableDataWithClass(
                                "location",
                                location.jarFileName.replace("-", NON_BREAKING_HYPHEN),
                                location.className.withTag("code"),
                                location.fieldName.withTag("code")
                        )
                }
            }
        }
    }

}