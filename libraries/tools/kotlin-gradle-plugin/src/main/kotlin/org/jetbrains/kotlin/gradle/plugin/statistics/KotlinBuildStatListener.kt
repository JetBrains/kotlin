/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.lang.management.ManagementFactory
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

class KotlinBuildStatListener {
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

        internal fun prepareData(
            event: TaskFinishEvent,
            projectName: String,
            uuid: String,
            label: String?,
            kotlinVersion: String
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
            val buildTimesMs = taskExecutionResult?.buildMetrics?.buildTimes?.asMapMs()?.filterValues { value -> value != 0L } ?: emptyMap()
            val perfData =
                taskExecutionResult?.buildMetrics?.buildPerformanceMetrics?.asMap()?.filterValues { value -> value != 0L } ?: emptyMap()
            val changes = when (val changedFiles = taskExecutionResult?.taskInfo?.changedFiles) {
                is ChangedFiles.Known -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
                is ChangedFiles.Dependencies -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
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
                tags = parseTags(taskExecutionResult).map { it.name },
                nonIncrementalAttributes = taskExecutionResult?.buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys ?: emptySet(),
                hostName = hostName,
                kotlinVersion = kotlinVersion,
                buildUuid = uuid,
                finishTime = System.currentTimeMillis(),
                compilerArguments = taskExecutionResult?.taskInfo?.compilerArguments?.asList() ?: emptyList()
            )
        }

        private fun parseTags(taskExecutionResult: TaskExecutionResult?): List<StatTag> {
            val tags = ArrayList<StatTag>()

            val nonIncrementalAttributes = taskExecutionResult?.buildMetrics?.buildAttributes?.asMap() ?: emptyMap()

            if (nonIncrementalAttributes.isEmpty()) {
                tags.add(StatTag.INCREMENTAL)
            } else {
                tags.add(StatTag.NON_INCREMENTAL)
            }

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

    }

}


