/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildAttributes
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimes
import org.jetbrains.kotlin.build.report.metrics.COMPILE_ITERATION
import org.jetbrains.kotlin.build.report.metrics.GRADLE_TASK_ACTION
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TaskExecutionResultTest {

    private fun taskExecutionResult(
        buildMetrics: BuildMetrics<BuildTimeMetric, BuildPerformanceMetric> = BuildMetrics(),
        taskInfo: TaskExecutionInfo = TaskExecutionInfo(),
        icLogLines: List<String> = emptyList(),
    ) = TaskExecutionResult(buildMetrics, taskInfo, icLogLines)

    @Test
    fun `merge combines build metrics from both results`() {
        val first = taskExecutionResult(
            buildMetrics = BuildMetrics(
                buildTimes = BuildTimes<BuildTimeMetric>().apply { addTimeMs(GRADLE_TASK_ACTION, 100) },
                buildAttributes = BuildAttributes().apply { add(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND) },
            )
        )
        val second = taskExecutionResult(
            buildMetrics = BuildMetrics(
                buildPerformanceMetrics = BuildPerformanceMetrics<BuildPerformanceMetric>().apply { addLong(COMPILE_ITERATION, 3) },
                buildAttributes = BuildAttributes().apply { add(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND, 2) },
            )
        )

        val merged = first.merge(second)

        assertEquals(100, merged.buildMetrics.buildTimes.buildTimesMapMs()[GRADLE_TASK_ACTION])
        assertEquals(3, merged.buildMetrics.buildPerformanceMetrics.asMap()[COMPILE_ITERATION])
        assertEquals(3, merged.buildMetrics.buildAttributes.asMap()[BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND])
    }

    @Test
    fun `merge prefers this kotlinLanguageVersion and changedFiles when present`() {
        val first = taskExecutionResult(
            taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = KotlinVersion.KOTLIN_2_4,
                changedFiles = SourcesChanges.ToBeCalculated,
            )
        )
        val second = taskExecutionResult(
            taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = KotlinVersion.KOTLIN_2_3,
                changedFiles = SourcesChanges.Unknown,
            )
        )

        val merged = first.merge(second)

        assertEquals(KotlinVersion.KOTLIN_2_4, merged.taskInfo.kotlinLanguageVersion)
        assertSame(SourcesChanges.ToBeCalculated, merged.taskInfo.changedFiles)
    }

    @Test
    fun `merge falls back to other kotlinLanguageVersion and changedFiles when this is null`() {
        val first = taskExecutionResult(taskInfo = TaskExecutionInfo(kotlinLanguageVersion = null, changedFiles = null))
        val second = taskExecutionResult(
            taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = KotlinVersion.KOTLIN_2_3,
                changedFiles = SourcesChanges.Unknown,
            )
        )

        val merged = first.merge(second)

        assertEquals(KotlinVersion.KOTLIN_2_3, merged.taskInfo.kotlinLanguageVersion)
        assertSame(SourcesChanges.Unknown, merged.taskInfo.changedFiles)
    }

    @Test
    fun `merge keeps this compilerArguments when non-empty, otherwise falls back to other`() {
        val nonEmptyArgs = arrayOf("-language-version", "2.0")
        val otherArgs = arrayOf("-language-version", "1.9")

        val withArgumentsMerged = taskExecutionResult(taskInfo = TaskExecutionInfo(compilerArguments = nonEmptyArgs))
            .merge(taskExecutionResult(taskInfo = TaskExecutionInfo(compilerArguments = otherArgs)))
        val emptyArgumentsMerged = taskExecutionResult(taskInfo = TaskExecutionInfo(compilerArguments = emptyArray()))
            .merge(taskExecutionResult(taskInfo = TaskExecutionInfo(compilerArguments = otherArgs)))

        assertEquals(nonEmptyArgs.toList(), withArgumentsMerged.taskInfo.compilerArguments.toList())
        assertEquals(otherArgs.toList(), emptyArgumentsMerged.taskInfo.compilerArguments.toList())
    }

    @Test
    fun `merge unions tags from both results`() {
        val first = taskExecutionResult(taskInfo = TaskExecutionInfo(tags = setOf(StatTag.ARTIFACT_TRANSFORM)))
        val second = taskExecutionResult(taskInfo = TaskExecutionInfo(tags = setOf(StatTag.INCREMENTAL)))

        val merged = first.merge(second)

        assertEquals(setOf(StatTag.ARTIFACT_TRANSFORM, StatTag.INCREMENTAL), merged.taskInfo.tags)
    }

    @Test
    fun `merge concatenates icLogLines preserving order`() {
        val first = taskExecutionResult(icLogLines = listOf("line1", "line2"))
        val second = taskExecutionResult(icLogLines = listOf("line3"))

        val merged = first.merge(second)

        assertEquals(listOf("line1", "line2", "line3"), merged.icLogLines)
    }
}
