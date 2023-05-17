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
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFlowService
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.plugin.statistics.MetricContainer
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.report.BuildScanExtensionHolder
import javax.inject.Inject

open class FlowParameterHolder @Inject constructor(private val flowScope: FlowScope, private val flowProviders: FlowProviders) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(FlowParameterHolder::class.java)
    }

    fun subscribeForBuildResult(project: Project) {
        val buildScanExtension = project.rootProject.extensions.findByName("buildScan")
        val buildScan = buildScanExtension?.let { BuildScanExtensionHolder(it) }
        val configurationTimeMetrics = project.provider {
            KotlinBuildStatsService.getInstance()?.buildStartedMetrics(project)
        }
        flowScope.always(
            BuildFinishFlowAction::class.java
        ) { spec ->
            spec.parameters.buildScanExtensionHolder.set(buildScan)
            spec.parameters.configurationTimeMetrics.set(configurationTimeMetrics)
            flowProviders.buildWorkResult.map { it.failure.isPresent }.let {
                spec.parameters.buildFailed.set(it)
            }
        }
    }
}


class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildFlowServiceProperty: Property<BuildFlowService>
        @get:ServiceReference
        val buildMetricService: Property<BuildMetricsService?>

        @get:Input
        val action: Property<String?>
        @get:Input
        val buildFailed: Property<Boolean>
        @get: Input
        val buildScanExtensionHolder: Property<BuildScanExtensionHolder?>
        @get: Input
        val configurationTimeMetrics: Property<MetricContainer>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildFlowServiceProperty.get().buildFinished(
            parameters.action.orNull, parameters.buildFailed.get(), parameters.configurationTimeMetrics.get()
        )
        parameters.buildMetricService.orNull?.addCollectedTagsToBuildScan(parameters.buildScanExtensionHolder.orNull)
    }
}
