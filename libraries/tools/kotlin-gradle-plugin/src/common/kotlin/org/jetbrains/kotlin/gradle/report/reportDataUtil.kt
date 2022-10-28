/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.lang.management.ManagementFactory
import java.util.ArrayList


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
    additionalTags: List<StatTag> = emptyList()
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
    val buildMetrics = buildOperationRecords.firstOrNull { it.path == taskPath }?.buildMetrics
    val taskExecutionResult = TaskExecutionResults[taskPath]
    val buildTimesMs = buildMetrics?.buildTimes?.asMapMs()?.filterValues { value -> value != 0L } ?: emptyMap()
    val perfData = buildMetrics?.buildPerformanceMetrics?.asMap()?.filterValues { value -> value != 0L } ?: emptyMap()
    val changes = when (val changedFiles = taskExecutionResult?.taskInfo?.changedFiles) {
        is ChangedFiles.Known -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
        else -> emptyList<String>()
    }
    return CompileStatisticsData(
        durationMs = durationMs,
        taskResult = taskResult.name,
        label = label,
        buildTimesMetrics = buildTimesMs,
        performanceMetrics = perfData,
        projectName = projectName,
        taskName = taskPath,
        changes = changes,
        tags = parseTags(taskExecutionResult, buildMetrics, additionalTags).map { it.name },
        nonIncrementalAttributes = buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys
            ?: emptySet(),
        hostName = BuildReportsService.hostName,
        kotlinVersion = kotlinVersion,
        buildUuid = uuid,
        finishTime = System.currentTimeMillis(),
        compilerArguments = taskExecutionResult?.taskInfo?.compilerArguments?.asList() ?: emptyList()
    )
}

private fun parseTags(taskExecutionResult: TaskExecutionResult?, buildMetrics: BuildMetrics?, additionalTags: List<StatTag>): List<StatTag> {
    val tags = parseTags(taskExecutionResult, additionalTags)
    val nonIncrementalAttributes = buildMetrics?.buildAttributes?.asMap() ?: emptyMap()
    if (nonIncrementalAttributes.isEmpty()) {
        tags.add(StatTag.INCREMENTAL)
    } else {
        tags.add(StatTag.NON_INCREMENTAL)
    }
    return tags
}

private fun parseTags(
    taskExecutionResult: TaskExecutionResult?,
    additionalTags: List<StatTag>,
): MutableList<StatTag>{
    val tags = ArrayList(additionalTags)
    val taskInfo = taskExecutionResult?.taskInfo

    taskInfo?.withAbiSnapshot?.ifTrue {
        tags.add(StatTag.ABI_SNAPSHOT)
    }
    taskInfo?.withArtifactTransform?.ifTrue {
        tags.add(StatTag.ARTIFACT_TRANSFORM)
    }

    val debugConfiguration = "-agentlib:"
    if (ManagementFactory.getRuntimeMXBean().inputArguments.firstOrNull { it.startsWith(debugConfiguration) } != null) {
        tags.add(StatTag.GRADLE_DEBUG)
    }
    return tags
}