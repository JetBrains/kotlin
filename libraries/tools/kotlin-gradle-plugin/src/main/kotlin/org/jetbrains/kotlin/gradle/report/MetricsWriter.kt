/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.gradle.internal.build.metrics.GradleBuildMetricsData
import org.jetbrains.kotlin.gradle.internal.build.metrics.TaskData
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import java.io.File
import java.io.ObjectOutputStream

internal class MetricsWriter(
    private val outputFile: File,
) : BuildExecutionDataProcessor {
    override fun process(build: BuildExecutionData, log: Logger) {
        if (build.failureMessages.isNotEmpty()) return

        try {
            outputFile.parentFile?.apply { mkdirs() }

            val buildMetricsData = GradleBuildMetricsData()
            for (metric in BuildTime.values()) {
                buildMetricsData.parentMetric[metric.name] = metric.parent?.name
            }
            for (attr in BuildAttribute.values()) {
                buildMetricsData.buildAttributeKind[attr.name] = attr.kind.name
            }

            for (data in build.taskExecutionData) {
                val path = data.taskPath
                val type = data.type
                val buildTimes = data.buildMetrics.buildTimes.asMap().mapKeys { (k, _) -> k.name }
                val buildPerfMetrics = data.buildMetrics.buildPerformanceMetrics.asMap().mapKeys { (k, _) -> k.name }
                val buildAttributes = data.buildMetrics.buildAttributes.asMap().mapKeys { (k, _) -> k.name }
                buildMetricsData.taskData[path] =
                    TaskData(
                        path = path,
                        typeFqName = type,
                        timeMetrics = buildTimes,
                        performanceMetrics = buildPerfMetrics,
                        buildAttributes = buildAttributes,
                        didWork = data.didWork
                    )
            }

            ObjectOutputStream(outputFile.outputStream().buffered()).use { out ->
                out.writeObject(buildMetricsData)
            }
        } catch (e: Exception) {
            log.kotlinDebug { "Could not write metrics to $outputFile: $e" }
        }
    }
}