/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.internal.report.BuildScanApi
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFinishBuildService
import org.jetbrains.kotlin.gradle.plugin.statistics.ConfigurationMetricParameterFlowActionBuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.FlowActionBuildFusService
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

    fun subscribeForBuildResultAndConfigurationTimeMetrics(
        buildFusServiceProvider: Provider<FlowActionBuildFusService>,
        buildFinishBuildService: Provider<BuildFinishBuildService>?,
    ) {
        flowScope.always(
            BuildFinishAndConfigurationTimeMetricsFlowAction::class.java
        ) { spec ->
            spec.parameters.buildFailed.set(flowProviders.buildWorkResult.map { it.failure.isPresent })
            spec.parameters.configurationTimeMetrics.addAll(buildFusServiceProvider.get().getConfigurationTimeMetrics())
            //Gradle 9+: ensure BuildFinishBuildService is initialized at the same time as BuildFusService to ensure the same buildId is used
            buildFinishBuildService?.get()
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

        @get:Input
        val buildFailed: Property<Boolean>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildFusServiceProperty.orNull?.recordBuildFinished(
            parameters.buildFailed.get(),
            parameters.buildFusServiceProperty.orNull?.parameters?.configurationMetrics?.orNull ?: emptyList()
        )
    }
}

internal class BuildFinishAndConfigurationTimeMetricsFlowAction : FlowAction<BuildFinishAndConfigurationTimeMetricsFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildFusServiceProperty: Property<FlowActionBuildFusService>

        @get:Input
        val buildFailed: Property<Boolean>

        @get:Input
        val configurationTimeMetrics: ListProperty<MetricContainer>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildFusServiceProperty.orNull?.recordBuildFinished(
            parameters.buildFailed.get(),
            parameters.configurationTimeMetrics.get()
        )
    }
}
