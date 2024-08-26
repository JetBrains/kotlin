/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.flow.*
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.report.BuildScanApi
import org.jetbrains.kotlin.gradle.report.BuildScanExtensionHolder
import javax.inject.Inject

internal abstract class StatisticsBuildFlowManager @Inject constructor(
    private val flowScope: FlowScope,
    private val flowProviders: FlowProviders,
) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(StatisticsBuildFlowManager::class.java)
    }

    fun subscribeForBuildResult() {
        flowScope.always(
            BuildFinishFlowAction::class.java
        ) { spec ->
            spec.parameters.buildFailed.set(flowProviders.buildWorkResult.map { it.failure.isPresent })
        }
    }

    fun subscribeForBuildScan(buildScan: BuildScanApi) {
        flowScope.always(
            BuildScanFlowAction::class.java
        ) { spec ->
            spec.parameters.buildScan.set(buildScan)
        }
    }
}

internal class BuildScanFlowAction : FlowAction<BuildScanFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildMetricService: Property<BuildMetricsService>

        @get:Input
        val buildScan: Property<BuildScanApi>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildMetricService.orNull?.addBuildScanReport(parameters.buildScan.orNull)
    }
}

internal class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildFusServiceProperty: Property<BuildFusService>

        @get:Input
        val buildFailed: Property<Boolean>

    }

    override fun execute(parameters: Parameters) {
        parameters.buildFusServiceProperty.orNull?.recordBuildFinished(parameters.buildFailed.get())
    }
}
