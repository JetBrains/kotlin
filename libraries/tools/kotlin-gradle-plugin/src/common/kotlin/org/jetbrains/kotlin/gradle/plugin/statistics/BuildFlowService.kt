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
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.IStatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.Serializable

abstract class BuildFlowService : BuildService<BuildFlowService.Parameters>, AutoCloseable, OperationCompletionListener {
    private var buildFailed: Boolean = false


    interface Parameters : BuildServiceParameters {
        val configurationMetrics: Property<MetricContainer>
    }
    companion object {
        fun registerIfAbsentImpl(
            project: Project,
        ): Provider<BuildFlowService> {
            return project.gradle.registerClassLoaderScopedBuildService(BuildFlowService::class) { buidService ->
                project.gradle.projectsEvaluated {
                    buidService.parameters.configurationMetrics.set(project.provider {
                        KotlinBuildStatsService.getInstance()?.buildStartedMetrics(project)
                    })
                }
            }.also {
                KotlinBuildStatsService.applyIfInitialised {
                    it.projectsEvaluated(project.gradle)
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
        buildFinished(null, buildFailed, parameters.configurationMetrics.get())
    }

    internal fun buildFinished(action: String?, buildFailed: Boolean, configurationTimeMetrics: MetricContainer) {
        KotlinBuildStatsService.applyIfInitialised {
            it.buildFinished(action, buildFailed, configurationTimeMetrics)
        }
    }
}

class MetricContainer : Serializable {
    private val numericalMetrics = HashMap<NumericalMetrics, Long>()
    private val booleanMetrics = HashMap<BooleanMetrics, Boolean>()
    private val stringMetrics = HashMap<StringMetrics, String>()

    fun report(sessionLogger: IStatisticsValuesConsumer) {
        for ((key, value) in numericalMetrics) {
            sessionLogger.report(key, value)
        }
        for ((key, value) in booleanMetrics) {
            sessionLogger.report(key, value)
        }
        for ((key, value) in stringMetrics) {
            sessionLogger.report(key, value)
        }
    }

    fun put(metric: StringMetrics, value: String) = stringMetrics.put(metric, value)
    fun put(metric: BooleanMetrics, value: Boolean) = booleanMetrics.put(metric, value)
    fun put(metric: NumericalMetrics, value: Long) = numericalMetrics.put(metric, value)
}