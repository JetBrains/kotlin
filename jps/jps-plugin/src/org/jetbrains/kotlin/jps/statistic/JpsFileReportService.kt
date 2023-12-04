/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import org.jetbrains.kotlin.build.report.metrics.JpsBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.JpsBuildTime
import org.jetbrains.kotlin.build.report.statistics.CompileStatisticsData
import org.jetbrains.kotlin.build.report.statistics.file.FileReportService
import org.jetbrains.kotlin.compilerRunner.JpsKotlinLogger
import java.io.File
import kotlin.math.min

internal class JpsFileReportService(
    buildReportDir: File,
    projectName: String,
    printMetrics: Boolean,
    logger: JpsKotlinLogger,
    private val changedFileListPerLimit: Int?
) : FileReportService<JpsBuildTime, JpsBuildPerformanceMetric>(buildReportDir, projectName, printMetrics, logger) {
    override fun printCustomTaskMetrics(statisticsData: CompileStatisticsData<JpsBuildTime, JpsBuildPerformanceMetric>) {
        val changedFiles = statisticsData.getChanges().let { changes ->
            changedFileListPerLimit?.let { changes.subList(0, min(it, changes.size)) } ?: changes
        }
        p.println("Changed files: ${changedFiles.sorted()}")
        p.println("Execution result: ${statisticsData.getTaskResult()}")
    }
}