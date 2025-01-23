/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.flow.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.internal.report.BuildScanApi
import org.jetbrains.kotlin.gradle.plugin.statistics.FlowActionBuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.ConfigurationMetricParameterFlowActionBuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.MetricContainer
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import javax.inject.Inject

internal abstract class StatisticsBuildFlowManager @Inject constructor(
    private val flowScope: FlowScope,
    private val flowProviders: FlowProviders,
) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(StatisticsBuildFlowManager::class.java)
    }

    fun subscribeForBuildResultAndConfigurationTimeMetrics(buildFusServiceProvider: Provider<FlowActionBuildFusService>) {
        flowScope.always(
            BuildFinishAndConfigurationTimeMetricsFlowAction::class.java
        ) { spec ->
            spec.parameters.buildFailed.set(flowProviders.buildWorkResult.map { it.failure.isPresent })
            spec.parameters.configurationTimeMetrics.addAll(buildFusServiceProvider.get().getConfigurationTimeMetrics())
        }
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
        val buildFusServiceProperty: Property<ConfigurationMetricParameterFlowActionBuildFusService>

        @get:ServiceReference
        val buildUidServiceProperty: Property<BuildUidService?>

        @get:Input
        val buildFailed: Property<Boolean>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildFusServiceProperty.orNull?.recordBuildFinished(
            parameters.buildFailed.get(),
            parameters.buildUidServiceProperty.orNull?.buildId ?: "unknown_id",
            parameters.buildFusServiceProperty.orNull?.parameters?.configurationMetrics?.orNull ?: emptyList()
        )
    }
}

internal class BuildFinishAndConfigurationTimeMetricsFlowAction : FlowAction<BuildFinishAndConfigurationTimeMetricsFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildFusServiceProperty: Property<FlowActionBuildFusService>

        @get:ServiceReference
        val buildUidServiceProperty: Property<BuildUidService?>

        @get:Input
        val buildFailed: Property<Boolean>

        @get:Input
        val configurationTimeMetrics: ListProperty<MetricContainer>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildFusServiceProperty.orNull?.recordBuildFinished(
            parameters.buildFailed.get(),
            parameters.buildUidServiceProperty.orNull?.buildId ?: "unknown_id",
            parameters.configurationTimeMetrics.get()
        )
    }
}
