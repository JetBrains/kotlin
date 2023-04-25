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
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.lang.management.ManagementFactory
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion


private fun availableForStat(taskPath: String): Boolean {
    return taskPath.contains("Kotlin") && (TaskExecutionResults[taskPath] != null)
}

internal fun prepareData(
    event: TaskFinishEvent,
    projectName: String,
    uuid: String,
    label: String?,
    kotlinVersion: String,
    buildOperationRecords: Collection<BuildOperationRecord>,
    additionalTags: List<StatTag> = emptyList(),
    metricsToShow: Set<String>? = null
): CompileStatisticsData? {
    val result = event.result
    val taskPath = event.descriptor.taskPath
    val durationMs = result.endTime - result.startTime
    val taskResult = when (result) {
        is TaskSuccessResult -> when {
            result.isFromCache -> TaskExecutionState.FROM_CACHE
            result.isUpToDate -> TaskExecutionState.UP_TO_DATE
            else -> TaskExecutionState.SUCCESS
        }

        is TaskSkippedResult -> TaskExecutionState.SKIPPED
        is TaskFailureResult -> TaskExecutionState.FAILED
        else -> TaskExecutionState.UNKNOWN
    }

    if (!availableForStat(taskPath)) {
        return null
    }
    val taskExecutionResult = TaskExecutionResults[taskPath]
    val buildMetrics = buildOperationRecords.firstOrNull { it.path == taskPath }?.buildMetrics

    val performanceMetrics = collectBuildPerformanceMetrics(taskExecutionResult, buildMetrics)
    val buildTimesMetrics = collectBuildMetrics(
        taskExecutionResult, buildMetrics, performanceMetrics, result.startTime,
        System.currentTimeMillis()
    )
    val buildAttributes = collectBuildAttributes(taskExecutionResult, buildMetrics)
    val changes = when (val changedFiles = taskExecutionResult?.taskInfo?.changedFiles) {
        is ChangedFiles.Known -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
        else -> emptyList<String>()
    }
    return CompileStatisticsData(
        durationMs = durationMs,
        taskResult = taskResult.name,
        label = label,
        buildTimesMetrics = filterMetrics(metricsToShow, buildTimesMetrics),
        performanceMetrics = filterMetrics(metricsToShow, performanceMetrics),
        projectName = projectName,
        taskName = taskPath,
        changes = changes,
        tags = collectTags(taskExecutionResult, buildMetrics, additionalTags),
        nonIncrementalAttributes = buildAttributes,
        hostName = BuildReportsService.hostName,
        kotlinVersion = kotlinVersion,
        kotlinLanguageVersion = taskExecutionResult?.taskInfo?.kotlinLanguageVersion,
        buildUuid = uuid,
        finishTime = System.currentTimeMillis(),
        compilerArguments = taskExecutionResult?.taskInfo?.compilerArguments?.asList() ?: emptyList(),
        gcCountMetrics = buildMetrics?.gcMetrics?.asGcCountMap(),
        gcTimeMetrics = buildMetrics?.gcMetrics?.asGcTimeMap()
    )
}

private fun <E : Enum<E>> filterMetrics(
    expectedMetrics: Set<String>?,
    buildTimesMetrics: Map<E, Long>
): Map<E, Long> = expectedMetrics?.let { buildTimesMetrics.filterKeys { metric -> it.contains(metric.name) } } ?: buildTimesMetrics

private fun collectBuildAttributes(taskExecutionResult: TaskExecutionResult?, buildMetrics: BuildMetrics?): Set<BuildAttribute> {
    val attributes = HashSet<BuildAttribute>()
    buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys?.also { attributes.addAll(it) }
    taskExecutionResult?.buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys?.also { attributes.addAll(it) }
    return attributes
}


private fun collectBuildPerformanceMetrics(
    taskExecutionResult: TaskExecutionResult?,
    buildMetrics: BuildMetrics?
): Map<BuildPerformanceMetric, Long> {
    val taskBuildPerformanceMetrics = HashMap<BuildPerformanceMetric, Long>()
    taskExecutionResult?.buildMetrics?.buildPerformanceMetrics?.asMap()?.let { taskBuildPerformanceMetrics.putAll(it) }
    buildMetrics?.buildPerformanceMetrics?.asMap()?.let { taskBuildPerformanceMetrics.putAll(it) }
    return taskBuildPerformanceMetrics.filterValues { value -> value != 0L }
}
private fun collectBuildMetrics(
    taskExecutionResult: TaskExecutionResult?,
    buildMetrics: BuildMetrics?,
    performanceMetrics: Map<BuildPerformanceMetric, Long>,
    gradleTaskStartTime: Long? = null,
    taskFinishEventTime: Long? = null,
): Map<BuildTime, Long> {
    val taskBuildMetrics = HashMap<BuildTime, Long>()
    taskExecutionResult?.buildMetrics?.buildTimes?.asMapMs()?.let { taskBuildMetrics.putAll(it) }
    buildMetrics?.buildTimes?.asMapMs()?.let { taskBuildMetrics.putAll(it) }
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
    taskExecutionResult: TaskExecutionResult?,
    buildMetrics: BuildMetrics?,
    additionalTags: List<StatTag>
): List<StatTag> {
    val tags = collectTags(taskExecutionResult, additionalTags)
    val nonIncrementalAttributes = collectBuildAttributes(taskExecutionResult, buildMetrics)
    if (nonIncrementalAttributes.isEmpty()) {
        tags.add(StatTag.INCREMENTAL)
    } else {
        tags.add(StatTag.NON_INCREMENTAL)
    }
    return tags
}

private fun collectTags(
    taskExecutionResult: TaskExecutionResult?,
    additionalTags: List<StatTag>,
): MutableList<StatTag> {
    val tags = ArrayList(additionalTags)
    val taskInfo = taskExecutionResult?.taskInfo

    taskInfo?.withAbiSnapshot?.ifTrue {
        tags.add(StatTag.ABI_SNAPSHOT)
    }
    taskInfo?.withArtifactTransform?.ifTrue {
        tags.add(StatTag.ARTIFACT_TRANSFORM)
    }
    taskInfo?.kotlinLanguageVersion?.also {
        tags.add(getLanguageVersionTag(it))
    }

    val debugConfiguration = "-agentlib:"
    if (ManagementFactory.getRuntimeMXBean().inputArguments.firstOrNull { it.startsWith(debugConfiguration) } != null) {
        tags.add(StatTag.GRADLE_DEBUG)
    }
    return tags
}

private fun getLanguageVersionTag(languageVersion: KotlinVersion): StatTag {
    return when {
        languageVersion < KotlinVersion.KOTLIN_2_0 -> StatTag.KOTLIN_1
        else -> StatTag.KOTLIN_2
    }
}