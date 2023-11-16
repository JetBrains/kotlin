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

internal class JpsFileReportService(
    buildReportDir: File,
    projectName: String,
    printMetrics: Boolean,
    logger: JpsKotlinLogger,
) : FileReportService<JpsBuildTime, JpsBuildPerformanceMetric>(buildReportDir, projectName, printMetrics, logger) {
    override fun printCustomTaskMetrics(statisticsData: CompileStatisticsData<JpsBuildTime, JpsBuildPerformanceMetric>) {
        p.print("Changed files: ${statisticsData.getChanges().sorted()}")
        p.print("Execution result: ${statisticsData.getTaskResult()}")
    }
}