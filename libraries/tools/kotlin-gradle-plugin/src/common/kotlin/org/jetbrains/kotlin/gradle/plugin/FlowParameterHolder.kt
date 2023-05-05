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
import javax.inject.Inject

open class FlowParameterHolder @Inject constructor(private val flowScope: FlowScope, private val flowProviders: FlowProviders) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(FlowParameterHolder::class.java)
    }

    fun subscribeForBuildResult() {
        flowScope.always(
            BuildFinishFlowAction::class.java
        ) { spec ->
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

        @get:Input
        val action: Property<String?>
        @get:Input
        val buildFailed: Property<Boolean>
    }

    override fun execute(parameters: Parameters) {
        parameters.buildFlowServiceProperty.get().buildFinished(
            parameters.action.orNull, parameters.buildFailed.get()
        )
    }
}
