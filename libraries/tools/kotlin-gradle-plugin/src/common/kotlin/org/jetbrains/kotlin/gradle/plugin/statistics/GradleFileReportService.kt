/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTimeMetric
import org.jetbrains.kotlin.build.report.statistics.file.ReadableFileReportService
import java.io.File

class GradleFileReportService(
    buildReportDir: File,
    projectName: String,
    printMetrics: Boolean,
) : ReadableFileReportService<GradleBuildTimeMetric, GradleBuildPerformanceMetric>(buildReportDir, projectName, printMetrics)