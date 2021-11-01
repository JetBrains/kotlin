/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import java.text.SimpleDateFormat
import java.util.*

internal fun reportingSettings(rootProject: Project): ReportingSettings {
    val properties = PropertiesProvider(rootProject)
    val buildReportMode =
        when {
            !properties.buildReportEnabled -> BuildReportMode.NONE
            properties.buildReportVerbose -> BuildReportMode.VERBOSE
            else -> BuildReportMode.SIMPLE
        }
    val metricsOutputFile = properties.singleBuildMetricsFile
    val buildReportDir = properties.buildReportDir ?: rootProject.buildDir.resolve("reports/kotlin-build")
    val includeMetricsInReport = properties.buildReportMetrics || buildReportMode == BuildReportMode.VERBOSE
    return ReportingSettings(
        metricsOutputFile = metricsOutputFile,
        buildReportDir = buildReportDir,
        reportMetrics = metricsOutputFile != null || includeMetricsInReport,
        includeMetricsInReport = includeMetricsInReport,
        buildReportMode = buildReportMode
    )
}
