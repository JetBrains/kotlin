/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.Project
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowScope
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.fus.Metric
import javax.inject.Inject


internal abstract class FusBuildFinishFlowManager @Inject constructor(
    private val flowScope: FlowScope,
) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(FusBuildFinishFlowManager::class.java)
    }

    fun subscribeForBuildFinish(fusService: Provider<BuildFlowFusStatisticsBuildService>, buildUid: Provider<String>) {
        flowScope.always(
            BuildFinishFlowAction::class.java
        ) {
            it.parameters.configurationTimeMetrics.addAll(fusService.get().getConfigurationReportedMetrics())
            it.parameters.buildId.set(buildUid)
        }
    }
}

class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val customFusServiceProperty: Property<BuildFlowFusStatisticsBuildService?>

        @get:Input
        val configurationTimeMetrics: ListProperty<Metric>

        @get:Input
        val buildId: Property<String>
    }

    private val log = Logging.getLogger(this.javaClass)

    override fun execute(parameters: Parameters) {
        parameters.customFusServiceProperty.orNull?.writeDownFusMetrics(parameters.buildId.get(), log, parameters.configurationTimeMetrics.orNull)
    }
}