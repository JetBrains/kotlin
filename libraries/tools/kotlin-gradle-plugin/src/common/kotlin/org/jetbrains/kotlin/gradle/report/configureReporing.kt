/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_BUILD_REPORT_SINGLE_FILE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_BUILD_REPORT_HTTP_URL

internal fun reportingSettings(rootProject: Project): ReportingSettings {
    val properties = PropertiesProvider(rootProject)
    val buildReportOutputTypes = properties.buildReportOutputs.map {
        BuildReportType.values().firstOrNull { brt -> brt.name == it.trim().toUpperCase() }
            ?: throw IllegalStateException("Unknown output type: $it")
    }.toMutableList() //temporary solution. support old property
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
            ?: throw IllegalStateException("Can't configure http report: '$KOTLIN_BUILD_REPORT_HTTP_URL' property is mandatory")
        val password = properties.buildReportHttpPassword
        val user = properties.buildReportHttpUser
        HttpReportSettings(url, password, user, properties.buildReportHttpVerboseEnvironment)
    } else {
        null
    }

    val buildScanSettings = if (buildReportOutputTypes.contains(BuildReportType.BUILD_SCAN)) {
        BuildScanSettings(properties.buildReportBuildScanCustomValuesLimit)
    } else {
        null
    }

    val singleOutputFile = if (buildReportOutputTypes.contains(BuildReportType.SINGLE_FILE)) {
        properties.buildReportSingleFile
            ?: throw IllegalStateException("Can't configure single file report: '$KOTLIN_BUILD_REPORT_SINGLE_FILE' property is mandatory")
    } else null

    //temporary solution. support old property
    val oldSingleBuildMetric = properties.singleBuildMetricsFile?.also { buildReportOutputTypes.add(BuildReportType.SINGLE_FILE) }

    return ReportingSettings(
        buildReportMode = buildReportMode,
        buildReportLabel = properties.buildReportLabel,
        fileReportSettings = fileReportSettings,
        httpReportSettings = httpReportSettings,
        buildScanReportSettings = buildScanSettings,
        buildReportOutputs = buildReportOutputTypes,
        singleOutputFile = singleOutputFile ?: oldSingleBuildMetric,
    )
}


