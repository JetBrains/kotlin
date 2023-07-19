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
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.incremental.ChangedFiles
import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.data.GradleCompileStatisticsData


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
    onlyKotlinTask: Boolean = true,
    additionalTags: Set<StatTag> = emptySet(),
    metricsToShow: Set<String>? = null
): GradleCompileStatisticsData? {
    val result = event.result
    val taskPath = event.descriptor.taskPath
    return prepareData(getTaskResult(event), taskPath, result.startTime, result.endTime - result.startTime, projectName, uuid,
                       label, kotlinVersion, buildOperationRecord, onlyKotlinTask, additionalTags, metricsToShow)
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
    onlyKotlinTask: Boolean = true,
    additionalTags: Set<StatTag> = emptySet(),
    metricsToShow: Set<String>? = null
): GradleCompileStatisticsData? {
    if (onlyKotlinTask && !(buildOperationRecord is TaskRecord && buildOperationRecord.isFromKotlinPlugin)) {
        return null
    }
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
    val kotlinLanguageVersion = if (buildOperationRecord is TaskRecord) buildOperationRecord.kotlinLanguageVersion else null

    return GradleCompileStatisticsData(
        durationMs = buildOperationRecord.totalTimeMs,
        taskResult = taskResult?.name,
        label = label,
        buildTimesMetrics = filterMetrics(metricsToShow, buildTimesMetrics),
        performanceMetrics = filterMetrics(metricsToShow, performanceMetrics),
        projectName = projectName,
        taskName = taskPath,
        changes = changes,
        tags = collectTags(buildOperationRecord, additionalTags),
        nonIncrementalAttributes = buildAttributes,
        hostName = BuildReportsService.hostName,
        kotlinVersion = kotlinVersion,
        kotlinLanguageVersion = kotlinLanguageVersion?.version,
        buildUuid = uuid,
        compilerArguments = collectCompilerArguments(buildOperationRecord),
        gcCountMetrics = buildMetrics.gcMetrics.asGcCountMap(),
        gcTimeMetrics = buildMetrics.gcMetrics.asGcTimeMap(),
        finishTime = finishTime,
        startTimeMs = startTime,
        fromKotlinPlugin = buildOperationRecord.isFromKotlinPlugin,
        skipMessage = buildOperationRecord.skipMessage,
        icLogLines = buildOperationRecord.icLogLines
    )
}

fun collectCompilerArguments(buildOperationRecord: BuildOperationRecord?): List<String> {
    return if (buildOperationRecord is TaskRecord) {
        buildOperationRecord.compilerArguments.asList()
    } else emptyList()
}

private fun <E : BuildTime> filterMetrics(
    expectedMetrics: Set<String>?,
    buildTimesMetrics: Map<E, Long>
): Map<E, Long> = expectedMetrics?.let { buildTimesMetrics.filterKeys { metric -> it.contains(metric.getName()) } } ?: buildTimesMetrics

private fun collectBuildAttributes(buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>?): Set<BuildAttribute> {
    return buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys ?: emptySet()
}


private fun collectBuildPerformanceMetrics(
    buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>?
): Map<GradleBuildPerformanceMetric, Long> {
    return buildMetrics?.buildPerformanceMetrics?.asMap()
        ?.filterValues { value -> value != 0L }
        ?.filterKeys { key ->
            key !in listOf(
                GradleBuildPerformanceMetric.START_WORKER_EXECUTION,
                GradleBuildPerformanceMetric.CALL_WORKER,
                GradleBuildPerformanceMetric.CALL_KOTLIN_DAEMON,
                GradleBuildPerformanceMetric.START_KOTLIN_DAEMON_EXECUTION
            )
        }
        ?: emptyMap()
}
private fun collectBuildMetrics(
    buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>?,
    gradleTaskStartTime: Long? = null,
    taskFinishEventTime: Long? = null,
): Map<GradleBuildTime, Long> {
    val taskBuildMetrics = HashMap<GradleBuildTime, Long>(buildMetrics?.buildTimes?.asMapMs())
    val performanceMetrics = buildMetrics?.buildPerformanceMetrics?.asMap() ?: emptyMap()
    gradleTaskStartTime?.let { startTime ->
        performanceMetrics[GradleBuildPerformanceMetric.START_TASK_ACTION_EXECUTION]?.let { actionStartTime ->
            taskBuildMetrics.put(GradleBuildTime.GRADLE_TASK_PREPARATION, actionStartTime - startTime)
        }
    }
    taskFinishEventTime?.let { listenerNotificationTime ->
        performanceMetrics[GradleBuildPerformanceMetric.FINISH_KOTLIN_DAEMON_EXECUTION]?.let { daemonFinishTime ->
            taskBuildMetrics.put(GradleBuildTime.TASK_FINISH_LISTENER_NOTIFICATION, listenerNotificationTime - daemonFinishTime)
        }
    }
    performanceMetrics[GradleBuildPerformanceMetric.CALL_WORKER]?.let { callWorkerTime ->
        performanceMetrics[GradleBuildPerformanceMetric.START_WORKER_EXECUTION]?.let { startWorkerExecutionTime ->
            taskBuildMetrics.put(GradleBuildTime.RUN_WORKER_DELAY, TimeUnit.NANOSECONDS.toMillis(startWorkerExecutionTime - callWorkerTime))
        }
    }
    return taskBuildMetrics.filterValues { value -> value != 0L }

}

private fun collectTags(
    buildOperation: BuildOperationRecord?,
    additionalTags: Set<StatTag>
): Set<StatTag> {
    val tags = HashSet(additionalTags)
    if (buildOperation is TaskRecord) {
        tags.addAll(collectTaskRecordTags(buildOperation))
    }

    val nonIncrementalAttributes = collectBuildAttributes(buildOperation?.buildMetrics)
    if (nonIncrementalAttributes.isEmpty()) {
        tags.add(StatTag.INCREMENTAL)
    } else {
        tags.add(StatTag.NON_INCREMENTAL)
    }

    return tags
}

private fun collectTaskRecordTags(
    taskRecord: TaskRecord?,
): Set<StatTag> {
    val tags = HashSet<StatTag>()

    taskRecord?.kotlinLanguageVersion?.also {
        tags.add(getLanguageVersionTag(it))
    }

    taskRecord?.statTags?.let { tags.addAll(it) }
    return tags
}

private fun getLanguageVersionTag(languageVersion: KotlinVersion): StatTag {
    return when {
        languageVersion < KotlinVersion.KOTLIN_2_0 -> StatTag.KOTLIN_1
        else -> StatTag.KOTLIN_2
    }
}