/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.fus.Metric
import java.io.File
import java.nio.file.Files
import java.util.*


private const val STATISTICS_FOLDER_NAME = "kotlin-profile"
private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"
private const val FILE_NAME_BUILD_ID_PREFIX_SEPARATOR = "."
private const val PROFILE_FILE_NAME_SUFFIX = ".plugin-profile"

internal fun <T : InternalGradleBuildFusStatisticsService.Parameter> InternalGradleBuildFusStatisticsService<T>.writeDownFusMetrics(
    buildId: String,
    log: Logger,
    configurationTimeMetrics: List<Metric>? = null,
) {
    val reportDir = File(parameters.fusStatisticsRootDirPath.get(), STATISTICS_FOLDER_NAME)
    try {
        Files.createDirectories(reportDir.toPath())
    } catch (e: Exception) {
        log.warn("Failed to create directory '$reportDir' for FUS report. FUS report won't be created", e)
        return
    }
    val reportFile = reportDir.createReportFile(
        buildId + FILE_NAME_BUILD_ID_PREFIX_SEPARATOR + UUID.randomUUID().toString(),
        PROFILE_FILE_NAME_SUFFIX,
        log
    ) ?: return

    reportFile.outputStream().bufferedWriter().use {
        it.appendLine("Build: $buildId")
        configurationTimeMetrics?.forEach { metric ->
            it.appendLine(metric.toString())
        }
        getExecutionTimeMetrics().get().forEach { reportedMetrics ->
            it.appendLine(reportedMetrics.toString())
        }
        it.appendLine(BUILD_SESSION_SEPARATOR)
    }
}

private const val MAX_ATTEMPTS = 20
private fun File.createReportFile(fileName: String, fileSuffix: String, log: Logger): File? {
    for (index in 0..MAX_ATTEMPTS) {
        val outputFile = resolve("$fileName-$index$fileSuffix")
        if (outputFile.createNewFile()) {
            return outputFile
        }
    }

    log.error("Failed to create report file after $MAX_ATTEMPTS attempts. FUS report for plugin won't be created.")
    return null
}