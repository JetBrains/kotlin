/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import java.text.SimpleDateFormat
import java.util.*

internal fun configureReporting(gradle: Gradle) {
    val buildDataProcessors = ArrayList<BuildExecutionDataProcessor>()

    val rootProject = gradle.rootProject
    val reportingSettings = reportingSettings(rootProject)
    gradle.taskGraph.whenReady { graph ->
        graph.allTasks.asSequence()
            .filterIsInstance<AbstractKotlinCompile<*>>()
            .forEach { it.reportingSettings = reportingSettings }
    }

    if (reportingSettings.buildReportMode != BuildReportMode.NONE && reportingSettings.buildReportDir != null) {
        configurePlainTextReportWriter(gradle, reportingSettings)?.let {
            buildDataProcessors.add(it)
        }
    }

    if (reportingSettings.metricsOutputFile != null) {
        buildDataProcessors.add(MetricsWriter(reportingSettings.metricsOutputFile.absoluteFile, rootProject.logger))
    }

    if (buildDataProcessors.isNotEmpty() && !isConfigurationCacheAvailable(gradle)) {
        val listener = BuildDataRecorder(gradle, buildDataProcessors)
        gradle.addBuildListener(listener)
    }
}

private fun reportingSettings(rootProject: Project): ReportingSettings {
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

private fun configurePlainTextReportWriter(
    gradle: Gradle,
    reportingSettings: ReportingSettings
): BuildExecutionDataProcessor? {
    val log = gradle.rootProject.logger
    val reportDir = reportingSettings.buildReportDir!!.apply { mkdirs() }
    if (reportDir.isFile) {
        log.error("Kotlin build report cannot be created: '$reportDir' is a file")
        return null
    }
    reportDir.mkdirs()
    val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
    val reportFile = reportDir.resolve("${gradle.rootProject.name}-build-$ts.txt")

    return PlainTextBuildReportWriter(
        outputFile = reportFile,
        printMetrics = reportingSettings.includeMetricsInReport,
        log = log
    )
}
