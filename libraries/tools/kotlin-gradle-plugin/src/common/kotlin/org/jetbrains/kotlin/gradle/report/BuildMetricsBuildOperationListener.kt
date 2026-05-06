/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.provider.Provider
import org.gradle.configuration.project.ConfigureProjectBuildOperationType
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.GRADLE_CONFIGURATION_TIME

internal class BuildMetricsBuildOperationListener(private val buildService: Provider<BuildMetricsService>) : BuildOperationListener, AutoCloseable {
    companion object {
        private const val CONFIGURATION = "configuration"
    }

    override fun finished(
        operationDescriptor: BuildOperationDescriptor?,
        event: OperationFinishEvent?,
    ) {
        val details = operationDescriptor?.details
        if (details is ConfigureProjectBuildOperationType.Details) {
            val buildMetrics: BuildMetrics<BuildTimeMetric, BuildPerformanceMetric> = BuildMetrics()
            buildMetrics.buildTimes.addTimeNs(GRADLE_CONFIGURATION_TIME, ((event?.endTime ?: 0) - (event?.startTime ?: 0)))
            buildService.get().addConfigurationRecord(
                getPath(details),
                BuildMetricsBuildOperationListener::class.java,
                event?.startTime ?: 0,
                (event?.endTime ?: 0) - (event?.startTime ?: 0),
                buildMetrics
            )
        }
    }

    private fun getPath(details: ConfigureProjectBuildOperationType.Details): String = when (details.projectPath) {
        ":" -> ":${CONFIGURATION}"
        else -> "${details.projectPath}:${CONFIGURATION}"
    }


    override fun progress(
        operationIdentifier: OperationIdentifier?,
        operationProgressEvent: OperationProgressEvent?,
    ) {
        //ignore
    }

    override fun started(
        operationDescriptor: BuildOperationDescriptor?,
        operationStartEvent: OperationStartEvent?,
    ) {
        //ignore
    }

    override fun close() {
    }

}
