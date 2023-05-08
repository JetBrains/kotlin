/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.flow.*
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import javax.inject.Inject

open class FlowParameterHolder @Inject constructor(val flowScope: FlowScope?, val flowProviders: FlowProviders?) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(FlowParameterHolder::class.java)
    }

    fun subscribeForBuildResult() {
        flowScope!!.always(
            BuildFinishFlowAction::class.java
        ) { spec ->
            spec.parameters.buildStatsServiceProperty.set(KotlinBuildStatsService.getInstance() as? KotlinBuildStatsService)
            flowProviders?.buildWorkResult?.orNull?.failure?.ifPresent { spec.parameters.failure.set(it) }
        }
    }
}


class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        val buildStatsServiceProperty: Property<KotlinBuildStatsService>
        val action: Property<String?>
        val failure: Property<Throwable?>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildStatsServiceProperty.get().buildFinished(
            parameters.action.orNull, parameters.failure.orNull
        )
    }
}
