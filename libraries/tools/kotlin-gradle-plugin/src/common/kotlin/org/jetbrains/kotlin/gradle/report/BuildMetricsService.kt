/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

internal interface UsesBuildMetricsService : Task {
    @get:Internal
    val buildMetricsService: Property<BuildMetricsService?>
}

abstract class BuildMetricsService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {

    private val log = Logging.getLogger(this.javaClass)

    // Tasks and transforms' records
    internal val buildOperationRecords = ConcurrentLinkedQueue<BuildOperationRecord>()
    internal val failureMessages = ConcurrentLinkedQueue<String>()

    // Info for tasks only
    private val taskPathToMetricsReporter = ConcurrentHashMap<String, BuildMetricsReporter>()
    private val taskPathToTaskClass = ConcurrentHashMap<String, String>()

    open fun addTask(taskPath: String, taskClass: Class<*>, metricsReporter: BuildMetricsReporter) {
        taskPathToMetricsReporter.put(taskPath, metricsReporter).also {
            if (it != null) log.warn("Duplicate task path: $taskPath") // Should never happen but log it just in case
        }
        taskPathToTaskClass.put(taskPath, taskClass.name).also {
            if (it != null) log.warn("Duplicate task path: $taskPath") // Should never happen but log it just in case
        }
    }

    open fun addTransformMetrics(
        transformPath: String,
        transformClass: Class<*>,
        isKotlinTransform: Boolean,
        startTimeMs: Long,
        totalTimeMs: Long,
        buildMetrics: BuildMetrics,
        failureMessage: String?
    ) {
        buildOperationRecords.add(
            TransformRecord(transformPath, transformClass.name, isKotlinTransform, startTimeMs, totalTimeMs, buildMetrics)
        )
        failureMessage?.let { failureMessages.add(it) }
    }

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val result = event.result
            val taskPath = event.descriptor.taskPath
            val totalTimeMs = result.endTime - result.startTime

            val buildMetrics = BuildMetrics()
            buildMetrics.buildTimes.addTimeMs(BuildTime.GRADLE_TASK, totalTimeMs)
            taskPathToMetricsReporter[taskPath]?.let {
                buildMetrics.addAll(it.getMetrics())
            }
            val taskExecutionResult = TaskExecutionResults[taskPath]
            taskExecutionResult?.buildMetrics?.also {
                buildMetrics.addAll(it)

                KotlinBuildStatsService.applyIfInitialised { collector ->
                    collector.report(NumericalMetrics.COMPILATION_DURATION, totalTimeMs)
                    collector.report(BooleanMetrics.KOTLIN_COMPILATION_FAILED, event.result is FailureResult)
                    val metricsMap = buildMetrics.buildPerformanceMetrics.asMap()

                    val linesOfCode = metricsMap[BuildPerformanceMetric.ANALYZED_LINES_NUMBER]
                    if (linesOfCode != null && linesOfCode > 0 && totalTimeMs > 0) {
                        collector.report(NumericalMetrics.COMPILED_LINES_OF_CODE, linesOfCode)
                        collector.report(NumericalMetrics.COMPILATION_LINES_PER_SECOND, linesOfCode * 1000 / totalTimeMs, null, linesOfCode)
                        metricsMap[BuildPerformanceMetric.ANALYSIS_LPS]?.also {
                            collector.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, it, null, linesOfCode)
                        }
                        metricsMap[BuildPerformanceMetric.CODE_GENERATION_LPS]?.also { value ->
                            collector.report(NumericalMetrics.CODE_GENERATION_LINES_PER_SECOND, value, null, linesOfCode)
                        }
                    }
                    collector.report(NumericalMetrics.COMPILATIONS_COUNT, 1)
                    collector.report(
                        NumericalMetrics.INCREMENTAL_COMPILATIONS_COUNT,
                        if (taskExecutionResult.buildMetrics.buildAttributes.asMap().isEmpty()) 1 else 0
                    )
                }
            }

            buildOperationRecords.add(
                TaskRecord(
                    path = taskPath,
                    classFqName = taskPathToTaskClass[taskPath] ?: "unknown",
                    startTimeMs = result.startTime,
                    totalTimeMs = totalTimeMs,
                    buildMetrics = buildMetrics,
                    didWork = result is TaskExecutionResult,
                    skipMessage = (result as? TaskSkippedResult)?.skipMessage,
                    icLogLines = taskExecutionResult?.icLogLines ?: emptyList(),
                    kotlinLanguageVersion = taskExecutionResult?.taskInfo?.kotlinLanguageVersion
                )
            )
            if (result is TaskFailureResult) {
                failureMessages.addAll(result.failures.map { it.message })
            }
        }
    }

    companion object {
        private val serviceClass = BuildMetricsService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

        private fun registerIfAbsentImpl(project: Project): Provider<BuildMetricsService>? {
            // Return early if the service was already registered to avoid the overhead of reading the reporting settings below
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<BuildMetricsService>
            }

            //do not need to collect metrics if there aren't consumers for this data
            val reportingSettings = reportingSettings(project)
            if (reportingSettings.buildReportOutputs.isEmpty()) {
                return null
            }

            return project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {}!!
        }

        fun registerIfAbsent(project: Project) = registerIfAbsentImpl(project)?.also { serviceProvider ->
            SingleActionPerProject.run(project, UsesBuildMetricsService::class.java.name) {
                project.tasks.withType<UsesBuildMetricsService>().configureEach { task ->
                    task.usesService(serviceProvider)
                }
            }
        }
    }

}

internal class TaskRecord(
    override val path: String,
    override val classFqName: String,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics,
    override val didWork: Boolean,
    override val skipMessage: String?,
    override val icLogLines: List<String>,
    val kotlinLanguageVersion: KotlinVersion?
) : BuildOperationRecord {
    override val isFromKotlinPlugin: Boolean = classFqName.startsWith("org.jetbrains.kotlin")
}

private class TransformRecord(
    override val path: String,
    override val classFqName: String,
    override val isFromKotlinPlugin: Boolean,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics
) : BuildOperationRecord {
    override val didWork: Boolean = true
    override val skipMessage: String? = null
    override val icLogLines: List<String> = emptyList()
}
