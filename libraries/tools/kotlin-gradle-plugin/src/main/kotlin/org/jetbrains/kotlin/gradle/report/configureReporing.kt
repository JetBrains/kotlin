/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider

internal fun reportingSettings(rootProject: Project): ReportingSettings {
    val properties = PropertiesProvider(rootProject)
    val buildReportOutputTypes = properties.buildReportOutputs.map {
        BuildReportType.values().firstOrNull { brt -> brt.name == it.trim().toUpperCase() }
            ?: throw IllegalStateException("Unknown output type: $it")
    }
    val buildReportMode =
        when {
            buildReportOutputTypes.isEmpty() -> BuildReportMode.NONE
            else -> BuildReportMode.VERBOSE
        }
    val fileReportSettings = if (buildReportOutputTypes.contains(BuildReportType.FILE)) {
        val buildReportDir = properties.buildReportFileOutputDir ?: rootProject.buildDir.resolve("reports/kotlin-build")
        val includeMetricsInReport = properties.buildReportMetrics || buildReportMode == BuildReportMode.VERBOSE
        FileReportSettings(buildReportDir = buildReportDir, includeMetricsInReport = includeMetricsInReport)
    } else {
        null
    }

    val httpReportSettings = if (buildReportOutputTypes.contains(BuildReportType.HTTP)) {
        val url = properties.buildReportHttpUrl
            ?: throw IllegalStateException("Can't configure http report: '${properties.buildReportHttpUrlProperty}' property is mandatory")
        val password = properties.buildReportHttpPassword
        val user = properties.buildReportHttpUser
        HttpReportSettings(url, password, user)
    } else {
        null
    }
    val metricsOutputFile = properties.singleBuildMetricsFile
    return ReportingSettings(
        metricsOutputFile = metricsOutputFile,
        buildReportMode = buildReportMode,
        fileReportSettings = fileReportSettings,
        httpReportSettings = httpReportSettings,
        buildReportOutputs = buildReportOutputTypes
    )
}


