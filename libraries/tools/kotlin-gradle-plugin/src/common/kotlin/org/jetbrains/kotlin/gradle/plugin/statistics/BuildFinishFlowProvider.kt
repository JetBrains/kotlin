/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class BuildFinishFlowProviderManager @Inject constructor(
    private val flowScope: FlowScope,
    private val flowProviders: FlowProviders,
) {
    companion object {
        internal fun getInstance(project: Project) =
            project.objects.newInstance(BuildFinishFlowProviderManager::class.java)
    }

    internal fun subscribeForBuildResults() {
        flowScope.always(
            BuildFinishFlowAction::class.java
        ) { spec ->
            spec.parameters.buildFailed.set(flowProviders.buildWorkResult.map { it.failure.isPresent })
        }
    }
}

internal abstract class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {

    interface Parameters : FlowParameters {
        @get:Input
        val buildFailed: Property<Boolean>

        @get:ServiceReference
        val fusBuildFinishServiceProperty: Property<BuildFinishBuildService>
    }

    override fun execute(parameters: Parameters) {
        parameters.fusBuildFinishServiceProperty.orNull?.collectAllFusReportsIntoOne()
    }
}