/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskExecutionResultsTest {

    @AfterTest
    fun cleanup() {
        TaskExecutionResults.clear()
    }

    private fun taskExecutionResult(
        kotlinLanguageVersion: KotlinVersion? = null,
        icLogLines: List<String> = emptyList(),
    ) = TaskExecutionResult(
        buildMetrics = BuildMetrics(),
        taskInfo = TaskExecutionInfo(kotlinLanguageVersion = kotlinLanguageVersion),
        icLogLines = icLogLines,
    )

    @Test
    fun `get returns null for a task path that was never set`() {
        assertNull(TaskExecutionResults[":unknown:task"])
    }

    @Test
    fun `set then get returns the stored result`() {
        val result = taskExecutionResult(icLogLines = listOf("line1"))

        TaskExecutionResults[":my:task"] = result

        assertEquals(listOf("line1"), TaskExecutionResults[":my:task"]?.icLogLines)
    }

    @Test
    fun `setting twice for the same task path merges results instead of overwriting`() {
        val first = taskExecutionResult(kotlinLanguageVersion = KotlinVersion.KOTLIN_2_4, icLogLines = listOf("line1"))
        val second = taskExecutionResult(kotlinLanguageVersion = KotlinVersion.KOTLIN_2_3, icLogLines = listOf("line2"))

        TaskExecutionResults[":my:task"] = first
        TaskExecutionResults[":my:task"] = second

        val merged = TaskExecutionResults[":my:task"]
        assertEquals(listOf("line1", "line2"), merged?.icLogLines)
        assertEquals(KotlinVersion.KOTLIN_2_4, merged?.taskInfo?.kotlinLanguageVersion)
    }

    @Test
    fun `different task paths are stored independently`() {
        val first = taskExecutionResult(icLogLines = listOf("first"))
        val second = taskExecutionResult(icLogLines = listOf("second"))

        TaskExecutionResults[":first:task"] = first
        TaskExecutionResults[":second:task"] = second

        assertEquals(listOf("first"), TaskExecutionResults[":first:task"]?.icLogLines)
        assertEquals(listOf("second"), TaskExecutionResults[":second:task"]?.icLogLines)
    }

    @Test
    fun `clear removes all stored results`() {
        TaskExecutionResults[":my:task"] = taskExecutionResult()

        TaskExecutionResults.clear()

        assertNull(TaskExecutionResults[":my:task"])
    }
}
