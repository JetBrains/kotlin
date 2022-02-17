/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleBuildServices
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import org.jetbrains.kotlin.gradle.report.data.TaskExecutionData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

abstract class BuildMetricsReporterService : BuildService<BuildMetricsReporterService.Parameters>,
    OperationCompletionListener, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        var buildDataProcessors: List<BuildExecutionDataProcessor>
        var startParameters: List<String>
        var reportingSettings: ReportingSettings
    }

    private val log = Logging.getLogger(this.javaClass)

    // Tasks and transforms' records
    private val taskAndTransformRecords = ConcurrentLinkedQueue<TaskExecutionData>()
    private val failureMessages = ConcurrentLinkedQueue<String>()

    // Info for tasks only
    // The list (ConcurrentLinkedQueue) should typically contain only 1 element, but it's not important to enforce that.
    private val taskPathToMetricsReporter = ConcurrentHashMap<String, ConcurrentLinkedQueue<BuildMetricsReporter>>()
    private val taskPathToTaskClass = ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>()

    open fun addTask(taskPath: String, taskClass: Class<*>, metricsReporter: BuildMetricsReporter) {
        taskPathToMetricsReporter.getOrPut(taskPath) { ConcurrentLinkedQueue() }.add(metricsReporter)
        taskPathToTaskClass.getOrPut(taskPath) { ConcurrentLinkedQueue() }.add(taskClass.name)
    }

    open fun addTransformMetrics(
        transformPath: String, transformClass: Class<*>, isKotlinTransform: Boolean, startTimeMs: Long, totalTimeMs: Long,
        buildMetrics: BuildMetrics, failureMessage: String?
    ) {
        taskAndTransformRecords.add(
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
            taskPathToMetricsReporter[taskPath]?.singleOrNull()?.let {
                buildMetrics.addAll(it.getMetrics())
            }
            val taskExecutionResult = TaskExecutionResults[taskPath]
            taskExecutionResult?.buildMetrics?.also { buildMetrics.addAll(it) }

            taskAndTransformRecords.add(
                TaskRecord(
                    taskPath = taskPath,
                    taskClass = taskPathToTaskClass[taskPath]?.let { it.singleOrNull() ?: "ambiguous" } ?: "unknown",
                    startTimeMs = result.startTime,
                    totalTimeMs = totalTimeMs,
                    buildMetrics = buildMetrics,
                    didWork = result is TaskExecutionResult,
                    skipMessage = (result as? TaskSkippedResult)?.skipMessage,
                    icLogLines = taskExecutionResult?.icLogLines ?: emptyList()
                )
            )
            if (result is TaskFailureResult) {
                failureMessages.addAll(result.failures.map { it.message })
            }
        }
    }

    override fun close() {
        val buildData = BuildExecutionData(
            startParameters = parameters.startParameters,
            failureMessages = failureMessages.toList(),
            taskExecutionData = taskAndTransformRecords.sortedBy { it.startTimeMs }
        )
        parameters.buildDataProcessors.forEach { it.process(buildData, log) }
    }

    companion object {

        fun getStartParameters(project: Project) = project.gradle.startParameter.let {
            val startParameters = arrayListOf<String>()
            startParameters.add("tasks = ${it.taskRequests.joinToString { task -> task.args.toString() }}")
            startParameters.add("excluded tasks = ${it.excludedTaskNames}")
            startParameters.add("current dir = ${it.currentDir}")
            startParameters.add("project properties args = ${it.projectProperties}")
            startParameters.add("system properties args = ${it.systemPropertiesArgs}")
            startParameters
        }

        fun registerIfAbsent(project: Project): Provider<BuildMetricsReporterService>? {
            val startParameters = getStartParameters(project)
            val rootProject = project.gradle.rootProject
            val reportingSettings = reportingSettings(rootProject)

            val buildDataProcessors = ArrayList<BuildExecutionDataProcessor>()
            reportingSettings.fileReportSettings?.let {
                buildDataProcessors.add(
                    PlainTextBuildReportWriterDataProcessor(
                        it,
                        rootProject.name
                    )
                )
            }

            if (reportingSettings.metricsOutputFile != null) {
                buildDataProcessors.add(MetricsWriter(reportingSettings.metricsOutputFile.absoluteFile))
            }

            if (reportingSettings.buildReportOutputs.isEmpty() && buildDataProcessors.isEmpty()) {
                return null
            }

            return project.gradle.sharedServices.registerIfAbsent(
                "build_metric_service_${KotlinGradleBuildServices::class.java.classLoader.hashCode()}",
                BuildMetricsReporterService::class.java
            ) {
                it.parameters.startParameters = startParameters
                it.parameters.buildDataProcessors = buildDataProcessors
                it.parameters.reportingSettings = reportingSettings
            }!!
        }

    }

}

private class TaskRecord(
    taskPath: String,
    taskClass: String,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics,
    override val didWork: Boolean,
    override val skipMessage: String?,
    override val icLogLines: List<String>
) : TaskExecutionData {
    override val taskOrTransformPath: String = taskPath
    override val taskOrTransformClass: String = taskClass
    override val isKotlinTaskOrTransform: Boolean = taskClass.startsWith("org.jetbrains.kotlin")
}

private class TransformRecord(
    transformPath: String,
    transformClass: String,
    isKotlinTransform: Boolean,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics
) : TaskExecutionData {
    override val taskOrTransformPath: String = transformPath
    override val taskOrTransformClass: String = transformClass
    override val isKotlinTaskOrTransform: Boolean = isKotlinTransform
    override val didWork: Boolean = true
    override val skipMessage: String? = null
    override val icLogLines: List<String> = emptyList()
}
