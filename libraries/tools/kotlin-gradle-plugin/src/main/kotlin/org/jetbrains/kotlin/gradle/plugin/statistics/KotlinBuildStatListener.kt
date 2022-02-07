/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.logging.Logging
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.net.InetAddress
import java.util.*
import kotlin.system.measureTimeMillis

enum class TaskExecutionState {
    SKIPPED,
    FAILED,
    UNKNOWN,
    SUCCESS,
    FROM_CACHE,
    UP_TO_DATE
    ;
}

class KotlinBuildStatListener(
    val projectName: String,
    val label: String?,
    val reportStatistics: List<ReportStatistics>
) : OperationCompletionListener, AutoCloseable {

    private val log = Logging.getLogger(this.javaClass)
    val buildUuid: String = UUID.randomUUID().toString()

    companion object {
        val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }

        private fun availableForStat(taskPath: String): Boolean {
            return taskPath.contains("Kotlin") && (TaskExecutionResults[taskPath] != null)
        }

        internal fun prepareData(event: TaskFinishEvent, projectName:String, uuid: String, label: String?): CompileStatData? {
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
            val buildTimesMs = taskExecutionResult?.buildMetrics?.buildTimes?.asMapMs()?.filterValues { value -> value != 0L } ?: emptyMap()
            val perfData =
                taskExecutionResult?.buildMetrics?.buildPerformanceMetrics?.asMap()?.filterValues { value -> value != 0L } ?: emptyMap()
            val changes = when (val changedFiles = taskExecutionResult?.taskInfo?.changedFiles) {
                is ChangedFiles.Known -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
                is ChangedFiles.Dependencies -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
                else -> emptyList<String>()

            }
            return CompileStatData(
                duration = durationMs, taskResult = taskResult.name, label = label,
                timeData = buildTimesMs, perfData = perfData, projectName = projectName, taskName = taskPath, changes = changes,
                tags = taskExecutionResult?.taskInfo?.properties?.map { it.name } ?: emptyList(),
                nonIncrementalAttributes = taskExecutionResult?.buildMetrics?.buildAttributes?.asMap() ?: emptyMap(),
                hostName = hostName, kotlinVersion = "1.6", buildUuid = uuid, timeInMillis = System.currentTimeMillis()
            )
        }

    }

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
                prepareData(event, projectName, buildUuid, label)
            }
            log.debug("Collect data takes $collectDataDuration: $compileStatData")

            val reportDataDuration = measureTimeMillis {
                compileStatData?.also { data -> reportStatistics.forEach { it.report(data) }}
            }
            log.debug("Report data takes $reportDataDuration: $compileStatData")
        }
    }

    override fun close() {
    }

}

