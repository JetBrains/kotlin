/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import org.jetbrains.kotlin.build.report.metrics.JpsBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.JpsBuildTime
import org.jetbrains.kotlin.build.report.statistics.CompileStatisticsData
import org.jetbrains.kotlin.build.report.statistics.file.Printer
import org.jetbrains.kotlin.build.report.statistics.file.ReadableFileReportService
import java.io.File
import kotlin.math.min

internal class JpsFileReportService(
    buildReportDir: File,
    projectName: String,
    printMetrics: Boolean,
    private val changedFileListPerLimit: Int?
) : ReadableFileReportService<JpsBuildTime, JpsBuildPerformanceMetric>(buildReportDir, projectName, printMetrics) {
    override fun printCustomTaskMetrics(statisticsData: CompileStatisticsData<JpsBuildTime, JpsBuildPerformanceMetric>, printer: Printer) {
        val changedFiles = statisticsData.getChanges().let { changes ->
            changedFileListPerLimit?.let { changes.subList(0, min(it, changes.size)) } ?: changes
        }
        printer.println("Changed files: ${changedFiles.sorted()}")
        printer.println("Execution result: ${statisticsData.getTaskResult()}")
    }
}