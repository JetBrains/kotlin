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
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
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

    fun subscribeForBuildScan(project: Project) {
        val buildScanExtension = project.rootProject.extensions.findByName("buildScan")
        val buildScanHolder = buildScanExtension?.let { BuildScanExtensionHolder(it) }

        flowScope.always(
            BuildScanFlowAction::class.java
        ) { spec ->
            spec.parameters.buildScanExtensionHolder.set(buildScanHolder)
        }
    }
}

internal class BuildScanFlowAction : FlowAction<BuildScanFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildMetricService: Property<BuildMetricsService?>

        @get: Input
        val buildScanExtensionHolder: Property<BuildScanExtensionHolder?>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildMetricService.orNull?.addBuildScanReport(parameters.buildScanExtensionHolder.orNull)
    }
}

internal class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val buildFlowServiceProperty: Property<BuildFlowService>

        @get:Input
        val action: Property<String?>

        @get:Input
        val buildFailed: Property<Boolean>

    }

    override fun execute(parameters: Parameters) {
        parameters.buildFlowServiceProperty.get().recordBuildFinished(
            parameters.action.orNull, parameters.buildFailed.get()
        )
    }
}
