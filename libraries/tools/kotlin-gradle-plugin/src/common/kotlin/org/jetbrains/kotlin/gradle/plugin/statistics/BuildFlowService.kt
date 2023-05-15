/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.FlowParameterHolder
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService

abstract class BuildFlowService : BuildService<BuildServiceParameters.None>, AutoCloseable, OperationCompletionListener {
    private var buildFailed: Boolean = false
    companion object {
        fun registerIfAbsentImpl(
            project: Project,
        ): Provider<BuildFlowService> {
            return project.gradle.registerClassLoaderScopedBuildService(BuildFlowService::class) {
            }.also {
                KotlinBuildStatsService.applyIfInitialised {
                    it.projectsEvaluated(project.gradle)
                    it.buildStarted(project.gradle)
                }
                if (GradleVersion.current().baseVersion < GradleVersion.version("8.1")) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(it)
                } else {
                    FlowParameterHolder.getInstance(project).subscribeForBuildResult(project)
                }
            }

        }
    }

    override fun onFinish(event: FinishEvent?) {
        if ((event is TaskFinishEvent) && (event.result is TaskFailureResult)) {
            buildFailed = true
        }
    }

    override fun close() {
        buildFinished(null, buildFailed)
    }

    internal fun buildFinished(action: String?, buildFailed: Boolean) {
        KotlinBuildStatsService.applyIfInitialised {
            it.buildFinished(action, buildFailed)
        }
    }
}