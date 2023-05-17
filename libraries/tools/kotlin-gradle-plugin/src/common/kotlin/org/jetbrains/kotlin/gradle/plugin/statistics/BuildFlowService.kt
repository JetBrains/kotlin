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
import org.jetbrains.kotlin.gradle.plugin.StatisticsBuildFlowManager
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.IStatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.Serializable

internal abstract class BuildFlowService : BuildService<BuildFlowService.Parameters>, AutoCloseable, OperationCompletionListener {
    private var buildFailed: Boolean = false


    interface Parameters : BuildServiceParameters {
        val configurationMetrics: Property<MetricContainer>
    }
    companion object {
        fun registerIfAbsentImpl(
            project: Project,
        ): Provider<BuildFlowService> {
            val buildService = project.gradle.registerClassLoaderScopedBuildService(BuildFlowService::class) { buidlService ->
                project.gradle.projectsEvaluated {
                    buidlService.parameters.configurationMetrics.set(project.provider {
                        KotlinBuildStatsService.getInstance()?.collectedStartMetrics(project)
                    })
                }
            }

            KotlinBuildStatsService.applyIfInitialised {
                it.recordProjectsEvaluated(project.gradle)
            }
            if (GradleVersion.current().baseVersion < GradleVersion.version("8.1")) {
                BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(buildService)
            } else {
                StatisticsBuildFlowManager.getInstance(project).subscribeForBuildResult(project)
            }

            return buildService
        }
    }

    override fun onFinish(event: FinishEvent?) {
        if ((event is TaskFinishEvent) && (event.result is TaskFailureResult)) {
            buildFailed = true
        }
    }

    override fun close() {
        recordBuildFinished(null, buildFailed, parameters.configurationMetrics.orElse(MetricContainer()).get())
    }

    internal fun recordBuildFinished(action: String?, buildFailed: Boolean, configurationTimeMetrics: MetricContainer) {
        KotlinBuildStatsService.applyIfInitialised {
            it.recordBuildFinish(action, buildFailed, configurationTimeMetrics)
        }
    }
}

internal class MetricContainer : Serializable {
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