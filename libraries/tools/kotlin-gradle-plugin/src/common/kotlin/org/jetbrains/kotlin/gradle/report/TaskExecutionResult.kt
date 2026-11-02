/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.util.HashSet

internal class TaskExecutionResult(
    val buildMetrics: BuildMetrics<BuildTimeMetric, BuildPerformanceMetric>,
    val taskInfo: TaskExecutionInfo = TaskExecutionInfo(),
    val icLogLines: List<String> = emptyList()
) {
    /**
     * Combines this result with [other], instead of one overwriting the other. This is needed because, for tasks
     * whose actual compiler/tool invocation is submitted to a Gradle worker without being awaited (see
     * [org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner.runToolWithWorkers]), the worker
     * can write its collected metrics into [org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults]
     * for the same task path either before or after the task action's own [TaskExecutionResult] (carrying
     * [taskInfo]) is written - so neither side can simply overwrite the other without potentially losing data.
     */
    fun merge(other: TaskExecutionResult): TaskExecutionResult {
        val mergedMetrics = BuildMetrics<BuildTimeMetric, BuildPerformanceMetric>()
        mergedMetrics.addAll(buildMetrics)
        mergedMetrics.addAll(other.buildMetrics)
        return TaskExecutionResult(
            buildMetrics = mergedMetrics,
            taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = taskInfo.kotlinLanguageVersion ?: other.taskInfo.kotlinLanguageVersion,
                changedFiles = taskInfo.changedFiles ?: other.taskInfo.changedFiles,
                compilerArguments = taskInfo.compilerArguments.ifEmpty { other.taskInfo.compilerArguments },
                tags = taskInfo.tags + other.taskInfo.tags,
            ),
            icLogLines = icLogLines + other.icLogLines,
        )
    }
}

internal class TaskExecutionInfo(
    val kotlinLanguageVersion: KotlinVersion? = null,
    val changedFiles: SourcesChanges? = null,
    val compilerArguments: Array<String> = emptyArray(),
    val tags: Set<StatTag> = emptySet(),
)

internal fun IncrementalCompilationEnvironment.collectIcTags(): Set<StatTag> = buildSet {
    if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
        add(StatTag.ARTIFACT_TRANSFORM)
    }
}
