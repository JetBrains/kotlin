/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.StatisticsBuildFlowManager
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheRequested
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationEnabled
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.report.reportingSettings
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.Serializable
import java.util.UUID.*


internal interface UsesBuildFusService : Task {
    @get:Internal
    val buildFusService: Property<BuildFusService?>
}

abstract class BuildFusService : BuildService<BuildFusService.Parameters>, AutoCloseable, OperationCompletionListener {
    private var buildFailed: Boolean = false
    private val log = Logging.getLogger(this.javaClass)

    init {
        log.kotlinDebug("Initialize ${this.javaClass.simpleName}")
    }

    interface Parameters : BuildServiceParameters {
        val configurationMetrics: ListProperty<MetricContainer>
        val useBuildFinishFlowAction: Property<Boolean>
    }

    private val fusMetricsConsumer = NonSynchronizedMetricsContainer()

    internal fun getFusMetricsConsumer(): StatisticsValuesConsumer = fusMetricsConsumer

    internal fun reportFusMetrics(reportAction: (StatisticsValuesConsumer) -> Unit) {
        reportAction(fusMetricsConsumer)
    }
    private val buildId = randomUUID().toString()

    companion object {
        private val serviceName = "${BuildFusService::class.simpleName}_${BuildFusService::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project, pluginVersion: String) =
            registerIfAbsentImpl(project, pluginVersion).also { serviceProvider ->
                SingleActionPerProject.run(project, UsesBuildFusService::class.java.name) {
                    project.tasks.withType<UsesBuildFusService>().configureEach { task ->
                        task.buildFusService.value(serviceProvider).disallowChanges()
                        task.usesService(serviceProvider)
                    }
                }
            }

        private fun registerIfAbsentImpl(
            project: Project,
            pluginVersion: String,
        ): Provider<BuildFusService> {

            val isProjectIsolationEnabled = project.isProjectIsolationEnabled

            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return (it.service as Provider<BuildFusService>).also {
                    it.get().parameters.configurationMetrics.add(project.provider {
                        KotlinBuildStatHandler.collectProjectConfigurationTimeMetrics(project)
                    })
                }
            }

            //init buildStatsService
            KotlinBuildStatsService.getOrCreateInstance(project)?.also {
                it.recordProjectsEvaluated(project)
            }

            val buildReportOutputs = reportingSettings(project).buildReportOutputs
            val gradle = project.gradle

            //Workaround for known issues for Gradle 8+: https://github.com/gradle/gradle/issues/24887:
            // when this OperationCompletionListener is called services can be already closed for Gradle 8,
            // so there is a change that no VariantImplementationFactory will be found
            return gradle.sharedServices.registerIfAbsent(serviceName, BuildFusService::class.java) { spec ->

                spec.parameters.configurationMetrics.add(project.provider {
                    KotlinBuildStatHandler.collectGeneralConfigurationTimeMetrics(
                        gradle,
                        buildReportOutputs,
                        pluginVersion,
                        isProjectIsolationEnabled
                    )
                })

                spec.parameters.configurationMetrics.add(project.provider {
                    KotlinBuildStatHandler.collectProjectConfigurationTimeMetrics(project)
                })
                spec.parameters.useBuildFinishFlowAction.set(GradleVersion.current().baseVersion >= GradleVersion.version("8.1"))
            }.also { buildService ->
                @Suppress("DEPRECATION")
                if (GradleVersion.current().baseVersion >= GradleVersion.version("7.4")
                    || !project.isConfigurationCacheRequested
                    || project.currentBuildId().name != "buildSrc"
                ) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(buildService)
                }
                if (GradleVersion.current().baseVersion >= GradleVersion.version("8.1")) {
                    StatisticsBuildFlowManager.getInstance(project).subscribeForBuildResult()
                }

            }
        }
    }

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            if (event.result is TaskFailureResult) {
                buildFailed = true
            }

            val taskExecutionResult = TaskExecutionResults[event.descriptor.taskPath]
            taskExecutionResult?.also { reportTaskMetrics(it, event) }
        }
    }

    override fun close() {
        if (!parameters.useBuildFinishFlowAction.get()) {
            recordBuildFinished(null, buildFailed)
        }
        log.kotlinDebug("Close ${this.javaClass.simpleName}")
    }

    internal fun recordBuildFinished(action: String?, buildFailed: Boolean) {
        KotlinBuildStatHandler.reportGlobalMetrics(fusMetricsConsumer)
        parameters.configurationMetrics.orElse(emptyList()).get().forEach { it.addToConsumer(fusMetricsConsumer) }
        KotlinBuildStatsService.applyIfInitialised {
            it.recordBuildFinish(action, buildFailed, fusMetricsConsumer, buildId)
        }
        KotlinBuildStatsService.closeServices()
    }
    private fun reportTaskMetrics(taskExecutionResult: TaskExecutionResult, event: TaskFinishEvent) {
        val totalTimeMs = event.result.endTime - event.result.startTime
        val buildMetrics = taskExecutionResult.buildMetrics
        fusMetricsConsumer.report(NumericalMetrics.COMPILATION_DURATION, totalTimeMs)
        fusMetricsConsumer.report(BooleanMetrics.KOTLIN_COMPILATION_FAILED, event.result is FailureResult)
        fusMetricsConsumer.report(NumericalMetrics.COMPILATIONS_COUNT, 1)

        val metricsMap = buildMetrics.buildPerformanceMetrics.asMap()

        val linesOfCode = metricsMap[GradleBuildPerformanceMetric.ANALYZED_LINES_NUMBER]
        if (linesOfCode != null && linesOfCode > 0 && totalTimeMs > 0) {
            fusMetricsConsumer.report(NumericalMetrics.COMPILED_LINES_OF_CODE, linesOfCode)
            fusMetricsConsumer.report(NumericalMetrics.COMPILATION_LINES_PER_SECOND, linesOfCode * 1000 / totalTimeMs, null, linesOfCode)
            metricsMap[GradleBuildPerformanceMetric.ANALYSIS_LPS]?.also { value ->
                fusMetricsConsumer.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, value, null, linesOfCode)
            }
            metricsMap[GradleBuildPerformanceMetric.CODE_GENERATION_LPS]?.also { value ->
                fusMetricsConsumer.report(NumericalMetrics.CODE_GENERATION_LINES_PER_SECOND, value, null, linesOfCode)
            }
        }
        fusMetricsConsumer.report(
            NumericalMetrics.INCREMENTAL_COMPILATIONS_COUNT,
            if (taskExecutionResult.buildMetrics.buildAttributes.asMap().isEmpty()) 1 else 0
        )
    }
}

class MetricContainer : Serializable {
    private val numericalMetrics = HashMap<NumericalMetrics, Long>()
    private val booleanMetrics = HashMap<BooleanMetrics, Boolean>()
    private val stringMetrics = HashMap<StringMetrics, String>()

    fun addToConsumer(metricsConsumer: StatisticsValuesConsumer) {
        for ((key, value) in numericalMetrics) {
            metricsConsumer.report(key, value)
        }
        for ((key, value) in booleanMetrics) {
            metricsConsumer.report(key, value)
        }
        for ((key, value) in stringMetrics) {
            metricsConsumer.report(key, value)
        }
    }

    fun put(metric: StringMetrics, value: String) = stringMetrics.put(metric, value)
    fun put(metric: BooleanMetrics, value: Boolean) = booleanMetrics.put(metric, value)
    fun put(metric: NumericalMetrics, value: Long) = numericalMetrics.put(metric, value)
}