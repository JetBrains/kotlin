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
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
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
    private val buildOperationRecords = ConcurrentLinkedQueue<BuildOperationRecord>()
    private val failureMessages = ConcurrentLinkedQueue<String>()

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
            taskExecutionResult?.buildMetrics?.also { buildMetrics.addAll(it) }

            buildOperationRecords.add(
                TaskRecord(
                    path = taskPath,
                    classFqName = taskPathToTaskClass[taskPath] ?: "unknown",
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
            buildOperationRecord = buildOperationRecords.sortedBy { it.startTimeMs }
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
            val serviceClass = BuildMetricsReporterService::class.java
            val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

            // Return early if the service was already registered to avoid the overhead of reading the reporting settings below
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<BuildMetricsReporterService>
            }

            val reportingSettings = reportingSettings(project.rootProject)
            if (reportingSettings.buildReportOutputs.isEmpty()
                && reportingSettings.fileReportSettings == null
                && reportingSettings.metricsOutputFile == null
            ) {
                return null
            }

            return project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                val buildDataProcessors = mutableListOf<BuildExecutionDataProcessor>()
                reportingSettings.fileReportSettings?.let { fileReportSettings ->
                    buildDataProcessors.add(PlainTextBuildReportWriterDataProcessor(fileReportSettings, project.rootProject.name))
                }
                reportingSettings.metricsOutputFile?.let { metricsOutputFile ->
                    buildDataProcessors.add(MetricsWriter(metricsOutputFile.absoluteFile))
                }

                it.parameters.startParameters = getStartParameters(project)
                it.parameters.buildDataProcessors = buildDataProcessors
                it.parameters.reportingSettings = reportingSettings
            }!!
        }

    }

}

private class TaskRecord(
    override val path: String,
    override val classFqName: String,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics,
    override val didWork: Boolean,
    override val skipMessage: String?,
    override val icLogLines: List<String>
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
