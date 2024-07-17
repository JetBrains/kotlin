/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal


import org.gradle.api.logging.Logging
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal abstract class InternalGradleBuildFusStatisticsService : GradleBuildFusStatisticsBuildService() {

    private val metrics = ConcurrentHashMap<Metric, Any>()
    private val log = Logging.getLogger(this.javaClass)

    //It is not possible to rely on BuildUidService in close() method
    private val buildId = UUID.randomUUID().toString()

    //since Gradle
    override fun close() {
        //since Gradle 8.1 flow action [BuildFinishFlowAction] is used to collect all metrics and write them down in a single file
        if (parameters.useBuildFinishFlowAction.get()) {
            return
        }
        val reportDir = File(parameters.fusStatisticsRootDirPath.get(), STATISTICS_FOLDER_NAME)
        try {
            Files.createDirectories(reportDir.toPath())
        } catch (e: Exception) {
            log.warn("Failed to create directory '$reportDir' for FUS report. FUS report won't be created", e)
            return
        }
        val reportFile = reportDir.resolve(buildId)
        reportFile.createNewFile()
        FileOutputStream(reportFile, true).bufferedWriter().use {
            for ((key, value) in metrics) {
                it.appendLine("$key=$value")
            }
            it.appendLine(BUILD_SESSION_SEPARATOR)
        }
    }


    override fun reportMetric(name: String, value: Boolean, subprojectName: String?) {
        internalReportMetric(name, value, subprojectName)
    }

    override fun reportMetric(name: String, value: String, subprojectName: String?) {
        internalReportMetric(name, value, subprojectName)
    }

    override fun reportMetric(name: String, value: Number, subprojectName: String?) {
        internalReportMetric(name, value, subprojectName)
    }

    private fun internalReportMetric(name: String, value: Any, subprojectName: String?) {
        val oldValue = metrics.getOrPut(Metric(name, subprojectName)) { value }
        if (oldValue != value) {
            log.warn("Try to override $name metric: current value is \"$oldValue\", new value is \"$value\"")
        }
    }

    companion object {
        private const val STATISTICS_FOLDER_NAME = "kotlin-fus"
        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"
    }
}