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
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.FlowParameterHolder

abstract class BuildFlowService : BuildService<BuildFlowService.Parameters>, AutoCloseable {
    interface Parameters: BuildServiceParameters {
        val useFlowParameters: Property<Boolean>
    }
    companion object {
        private val serviceClass = BuildFlowService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
        private var buildFailed: Boolean = false
        fun registerIfAbsentImpl(
            project: Project,
        ): Provider<BuildFlowService>? {
            // Return early if the service was already registered to avoid the overhead of reading the reporting settings below
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<BuildFlowService>
            }
            return project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                it.parameters.useFlowParameters.set(GradleVersion.current().baseVersion >= GradleVersion.version("8.1"))
                KotlinBuildStatsService.applyIfInitialised {
                    it.buildStarted(project.gradle)
                }
            }.also {
                if (GradleVersion.current().baseVersion < GradleVersion.version("8.1")) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(
                        project.provider {
                            OperationCompletionListener { event ->
                                if ((event is TaskFinishEvent) && (event.result is TaskFailureResult)) {
                                    buildFailed = true
                                }
                            }
                        }
                    )
                } else {
                    FlowParameterHolder.getInstance(project).subscribeForBuildResult(project)
                }
            }

        }
    }

    override fun close() {
        if (!parameters.useFlowParameters.get()) {
            buildFinished(null, buildFailed)
        }
    }

    internal fun buildFinished(action: String?, buildFailed: Boolean) {
        KotlinBuildStatsService.applyIfInitialised {
            it.buildFinished(action, buildFailed)
        }
    }
}