/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.Metric
import org.jetbrains.kotlin.gradle.fus.UniqueId
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue

abstract class InternalGradleBuildFusStatisticsService<T : InternalGradleBuildFusStatisticsService.Parameter> :
    GradleBuildFusStatisticsService<T>, AutoCloseable {

    interface Parameter : BuildServiceParameters {
        val fusStatisticsRootDirPath: Property<String>
        val buildId: Property<String>
    }

    companion object {
        private const val STATISTICS_FOLDER_NAME = "kotlin-profile"
        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"
        internal const val FILE_NAME_BUILD_ID_PREFIX_SEPARATOR = "."
        internal const val PROFILE_FILE_NAME_SUFFIX = ".plugin-profile"
    }

    internal val executionTimeMetrics = ConcurrentLinkedQueue<Metric>()
    internal val buildId: String = parameters.buildId.get()

    init {
        log.debug("Initialize build service $serviceName: class \"${this.javaClass.simpleName}\", build \"$buildId\"")
    }

    override fun reportMetric(name: String, value: Boolean, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    override fun reportMetric(name: String, value: String, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    override fun reportMetric(name: String, value: Number, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    private fun internalReportMetric(name: String, value: Any, uniqueId: UniqueId) {
        //all aggregations should be done on IDEA side
        executionTimeMetrics.add(Metric(name, value, uniqueId))
    }

    /**
     * Returns a list of collected metrics sets.
     *
     *
     * These sets are not going to be merged into one as no aggregation information is present here.
     * Non-thread safe
     */
    abstract fun getExecutionTimeMetrics(): Provider<List<Metric>>

    internal fun writeDownFusMetrics(
        configurationTimeMetrics: List<Metric>? = null,
    ) {
        val reportDir = File(parameters.fusStatisticsRootDirPath.get(), STATISTICS_FOLDER_NAME)
        try {
            Files.createDirectories(reportDir.toPath())
        } catch (e: Exception) {
            log.warn("Failed to create directory '$reportDir' for FUS report. FUS report won't be created", e)
            return
        }
        val reportFile = reportDir.createReportFile(buildId, log) ?: return

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

    override fun close() {
        log.debug("Close build service $serviceName: class \"${this.javaClass.simpleName}\", build \"$buildId\"")
    }

}