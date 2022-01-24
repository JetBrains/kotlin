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
import java.io.Serializable

internal class MetricsWriter(
    private val outputFile: File,
) : BuildExecutionDataProcessor, Serializable {
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
                buildMetricsData.taskData[data.taskPath] =
                    TaskData(
                        path = data.taskPath,
                        typeFqName = data.type,
                        buildTimesMs = data.buildMetrics.buildTimes.asMapMs().mapKeys { it.key.name },
                        performanceMetrics = data.buildMetrics.buildPerformanceMetrics.asMap().mapKeys { it.key.name },
                        buildAttributes = data.buildMetrics.buildAttributes.asMap().mapKeys { it.key.name },
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