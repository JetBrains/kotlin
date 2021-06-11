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

enum class TaskExecutionState {
    SKIPPED,
    FAILED,
    UNKNOWN,
    SUCCESS,
    FROM_CACHE,
    UP_TO_DATE
    ;
}

class KotlinBuildEsStatListener(val projectName: String) : OperationCompletionListener, AutoCloseable, TaskExecutionListener {
    val reportStatistics: ReportStatistics = ReportStatisticsToElasticSearch

    val label by lazy { CompilerSystemProperties.KOTLIN_STAT_LABEl_PROPERTY.value }

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val result = event.result
            val taskPath = event.descriptor.taskPath
            val duration = event.result.endTime - event.result.startTime
            val taskResult = when (result) {
                is TaskSuccessResult -> if (result.isFromCache) TaskExecutionState.FROM_CACHE
                else if (result.isUpToDate) TaskExecutionState.UP_TO_DATE
                else TaskExecutionState.SUCCESS

                is TaskSkippedResult -> TaskExecutionState.SKIPPED
                is TaskFailureResult -> TaskExecutionState.FAILED
                else -> TaskExecutionState.UNKNOWN
            }

            reportData(taskPath, duration, taskResult)
        }
    }

    private fun reportData(taskPath: String, duration: Long, taskResult: TaskExecutionState) {
        val statData = TaskExecutionResults[taskPath]?.buildMetrics?.buildTimes?.asMap()?.mapKeys { (key, _) -> key.name } ?: emptyMap()

        val compileStatData = CompileStatData(
            duration = duration, taskResult = taskResult.name, label = label,
            statData = statData, projectName = projectName, taskName = taskPath
        )
        reportStatistics.report(compileStatData)
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

