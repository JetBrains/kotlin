/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.lang.management.ManagementFactory
import java.util.ArrayList
import java.util.concurrent.TimeUnit


internal fun getTaskResult(event: TaskFinishEvent) = when (val result = event.result) {
        is TaskSuccessResult -> when {
            result.isFromCache -> TaskExecutionState.FROM_CACHE
            result.isUpToDate -> TaskExecutionState.UP_TO_DATE
            else -> TaskExecutionState.SUCCESS
        }

        is TaskSkippedResult -> TaskExecutionState.SKIPPED
        is TaskFailureResult -> TaskExecutionState.FAILED
        else -> TaskExecutionState.UNKNOWN
    }

internal fun prepareData(
    event: TaskFinishEvent,
    projectName: String,
    uuid: String,
    label: String?,
    kotlinVersion: String,
    buildOperationRecord: BuildOperationRecord,
    additionalTags: Set<StatTag> = emptySet(),
    metricsToShow: Set<String>? = null
): CompileStatisticsData {
    val result = event.result
    val taskPath = event.descriptor.taskPath
    return prepareData(getTaskResult(event), taskPath, result.startTime, result.endTime - result.startTime, projectName, uuid,
                       label, kotlinVersion, buildOperationRecord, additionalTags, metricsToShow)
}

internal fun prepareData(
    taskResult: TaskExecutionState?,
    taskPath: String,
    startTime: Long,
    finishTime: Long,
    projectName: String,
    uuid: String,
    label: String?,
    kotlinVersion: String,
    buildOperationRecord: BuildOperationRecord,
    additionalTags: Set<StatTag> = emptySet(),
    metricsToShow: Set<String>? = null
): CompileStatisticsData {
    val buildMetrics = buildOperationRecord.buildMetrics

    val performanceMetrics = collectBuildPerformanceMetrics(buildMetrics)
    val buildTimesMetrics = collectBuildMetrics(
        buildMetrics, startTime, System.currentTimeMillis()
    )
    val buildAttributes = collectBuildAttributes(buildMetrics)
    val changes = if (buildOperationRecord is TaskRecord && buildOperationRecord.changedFiles is ChangedFiles.Known) {
        buildOperationRecord.changedFiles.modified.map { it.absolutePath } + buildOperationRecord.changedFiles.removed.map { it.absolutePath }
    } else {
        emptyList<String>()
    }

    return CompileStatisticsData(
        durationMs = buildOperationRecord.totalTimeMs,
        taskResult = taskResult?.name,
        label = label,
        buildTimesMetrics = filterMetrics(metricsToShow, buildTimesMetrics),
        performanceMetrics = filterMetrics(metricsToShow, performanceMetrics),
        projectName = projectName,
        taskName = taskPath,
        changes = changes,
        tags = collectTags(buildOperationRecord, additionalTags).map { it.name },
        nonIncrementalAttributes = buildAttributes,
        hostName = BuildReportsService.hostName,
        kotlinVersion = kotlinVersion,
        buildUuid = uuid,
        compilerArguments = collectCompilerArguments(buildOperationRecord),
        gcCountMetrics = buildMetrics.gcMetrics.asGcCountMap(),
        gcTimeMetrics = buildMetrics.gcMetrics.asGcTimeMap(),
        finishTime = finishTime,
        startTimeMs = startTime,
        fromKotlinPlugin = buildOperationRecord.isFromKotlinPlugin,
        skipMessage = buildOperationRecord.skipMessage,
        icLogLines = buildOperationRecord.icLogLines,
    )
}

fun collectCompilerArguments(buildOperationRecord: BuildOperationRecord?): List<String> {
    return if (buildOperationRecord is TaskRecord) {
        buildOperationRecord.compilerArguments.asList()
    } else emptyList()
}

private fun <E : Enum<E>> filterMetrics(
    expectedMetrics: Set<String>?,
    buildTimesMetrics: Map<E, Long>
): Map<E, Long> = expectedMetrics?.let { buildTimesMetrics.filterKeys { metric -> it.contains(metric.name) } } ?: buildTimesMetrics

private fun collectBuildAttributes(buildMetrics: BuildMetrics?): Set<BuildAttribute> {
    return buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys ?: emptySet()
}


private fun collectBuildPerformanceMetrics(
    buildMetrics: BuildMetrics?
): Map<BuildPerformanceMetric, Long> {
    return buildMetrics?.buildPerformanceMetrics?.asMap()
        ?.filterValues { value -> value != 0L }
        ?.filterKeys { key ->
            key !in listOf(
                BuildPerformanceMetric.START_WORKER_EXECUTION,
                BuildPerformanceMetric.CALL_WORKER,
                BuildPerformanceMetric.CALL_KOTLIN_DAEMON,
                BuildPerformanceMetric.START_KOTLIN_DAEMON_EXECUTION
            )
        }
        ?: emptyMap()
}
private fun collectBuildMetrics(
    buildMetrics: BuildMetrics?,
    gradleTaskStartTime: Long? = null,
    taskFinishEventTime: Long? = null,
): Map<BuildTime, Long> {
    val taskBuildMetrics = HashMap<BuildTime, Long>(buildMetrics?.buildTimes?.asMapMs())
    val performanceMetrics = buildMetrics?.buildPerformanceMetrics?.asMap() ?: emptyMap()
    gradleTaskStartTime?.let { startTime ->
        performanceMetrics[BuildPerformanceMetric.START_TASK_ACTION_EXECUTION]?.let { actionStartTime ->
            taskBuildMetrics.put(BuildTime.GRADLE_TASK_PREPARATION, actionStartTime - startTime)
        }
    }
    taskFinishEventTime?.let { listenerNotificationTime ->
        performanceMetrics[BuildPerformanceMetric.FINISH_KOTLIN_DAEMON_EXECUTION]?.let { daemonFinishTime ->
            taskBuildMetrics.put(BuildTime.TASK_FINISH_LISTENER_NOTIFICATION, listenerNotificationTime - daemonFinishTime)
        }
    }
    performanceMetrics[BuildPerformanceMetric.CALL_WORKER]?.let { callWorkerTime ->
        performanceMetrics[BuildPerformanceMetric.START_WORKER_EXECUTION]?.let { startWorkerExecutionTime ->
            taskBuildMetrics.put(BuildTime.RUN_WORKER_DELAY, TimeUnit.NANOSECONDS.toMillis(startWorkerExecutionTime - callWorkerTime))
        }
    }
    return taskBuildMetrics.filterValues { value -> value != 0L }

}

private fun collectTags(
    buildOperation: BuildOperationRecord?,
    additionalTags: Set<StatTag>
): Set<StatTag> {
    val tags = HashSet(additionalTags)

    val debugConfiguration = "-agentlib:"
    if (ManagementFactory.getRuntimeMXBean().inputArguments.firstOrNull { it.startsWith(debugConfiguration) } != null) {
        tags.add(StatTag.GRADLE_DEBUG)
    }

    val nonIncrementalAttributes = collectBuildAttributes(buildOperation?.buildMetrics)
    if (nonIncrementalAttributes.isEmpty()) {
        tags.add(StatTag.INCREMENTAL)
    } else {
        tags.add(StatTag.NON_INCREMENTAL)
    }

    if (buildOperation is TaskRecord) {
        tags.addAll(buildOperation.tags)
    }
    return tags
}
