/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.*
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.incremental.ChangedFiles
import java.net.InetAddress
import java.util.*

enum class TaskExecutionState {
    SKIPPED,
    FAILED,
    UNKNOWN,
    SUCCESS,
    FROM_CACHE,
    UP_TO_DATE
    ;
}

class KotlinBuildEsStatListener(val projectName: String, val reportStatistics: List<ReportStatistics>) :
    OperationCompletionListener, AutoCloseable, TaskExecutionListener {

    val buildUuid: String = UUID.randomUUID().toString()

    val label by lazy { CompilerSystemProperties.KOTLIN_STAT_LABEl_PROPERTY.value }
    val hostName: String? by lazy {
        try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }
    }

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val result = event.result
            val taskPath = event.descriptor.taskPath
            val duration = event.result.endTime - event.result.startTime
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

            reportData(taskPath, duration, taskResult)
        }
    }

    private fun reportData(taskPath: String, duration: Long, taskResult: TaskExecutionState) {
        if (!availableForStat(taskPath)) {
            return;
        }
        val taskExecutionResult = TaskExecutionResults[taskPath]
        val timeData = taskExecutionResult?.buildMetrics?.buildTimes?.asMap()?.filterValues { value -> value != 0L } ?: emptyMap()
        val perfData = taskExecutionResult?.buildMetrics?.buildPerformanceMetrics?.asMap()?.filterValues { value -> value != 0L } ?: emptyMap()
        val changes = when (val changedFiles = taskExecutionResult?.taskInfo?.changedFiles) {
            is ChangedFiles.Known -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
            is ChangedFiles.Dependencies -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
            else -> emptyList<String>()

        }
        val compileStatData = CompileStatData(
            duration = duration, taskResult = taskResult.name, label = label,
            timeData = timeData, perfData = perfData, projectName = projectName, taskName = taskPath, changes = changes,
            tags = taskExecutionResult?.taskInfo?.properties?.map { it.name } ?: emptyList(),
            nonIncrementalAttributes = taskExecutionResult?.buildMetrics?.buildAttributes?.asMap() ?: emptyMap(),
            hostName = hostName, kotlinVersion = "1.6", buildUuid = buildUuid, timeInMillis = System.currentTimeMillis()
        )
        reportStatistics.forEach { it.report(compileStatData) }
    }

    private fun availableForStat(taskPath: String): Boolean {
        return taskPath.contains("Kotlin") && (TaskExecutionResults[taskPath] != null)
    }

    override fun afterExecute(task: Task, taskState: TaskState) {
        val taskResult = when {
            taskState.skipped -> TaskExecutionState.SKIPPED
            taskState.failure != null -> TaskExecutionState.FAILED
            taskState.upToDate -> TaskExecutionState.UP_TO_DATE
            taskState.didWork -> TaskExecutionState.SUCCESS
            taskState.executed -> TaskExecutionState.FROM_CACHE
            else -> TaskExecutionState.UNKNOWN
        }
        val taskPath = task.path
        reportData(taskPath, 0L, taskResult)
    }

    override fun close() {
    }

    override fun beforeExecute(p0: Task) {
        //Do nothing
    }

}

