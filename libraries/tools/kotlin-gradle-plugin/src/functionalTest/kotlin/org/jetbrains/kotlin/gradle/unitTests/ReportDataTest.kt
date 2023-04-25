/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.tooling.events.OperationDescriptor
import org.gradle.tooling.events.PluginIdentifier
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskOperationDescriptor
import org.gradle.tooling.events.task.TaskOperationResult
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.report.TaskRecord
import org.jetbrains.kotlin.gradle.report.prepareData
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.sign
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class ReportDataTest {
    private val kotlinTaskPath = "testKotlin"

    @Test
    @Suppress("DEPRECATION")
    fun testTags() {
        TaskExecutionResults[kotlinTaskPath] = TaskExecutionResult(
            buildMetrics = BuildMetrics(buildAttributes = BuildAttributes()),
            taskInfo = TaskExecutionInfo(kotlinLanguageVersion = KotlinVersion.KOTLIN_1_4),
            icLogLines = emptyList(),
        )
        val buildOperationRecord =
            taskRecord(BuildMetrics(buildAttributes = BuildAttributes().also { it.add(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND) }))
        val statisticData = prepareData(
            event = taskFinishEvent(),
            projectName = "test",
            uuid = "uuid",
            label = "label",
            kotlinVersion = "version",
            buildOperationRecords = listOf(buildOperationRecord),
            additionalTags = listOf(StatTag.KOTLIN_DEBUG)
        )

        assertNotNull(statisticData)
        assertTrue(statisticData.tags.contains(StatTag.KOTLIN_DEBUG))
        assertTrue(statisticData.tags.contains(StatTag.NON_INCREMENTAL))
        assertTrue(statisticData.tags.contains(StatTag.KOTLIN_1))
    }

    private fun taskRecord(buildMetrics: BuildMetrics) = TaskRecord(
        path = kotlinTaskPath,
        classFqName = "class name",
        startTimeMs = 10,
        totalTimeMs = 20,
        buildMetrics = buildMetrics,
        didWork = true,
        skipMessage = null,
        icLogLines = emptyList(),
        kotlinLanguageVersion = KotlinVersion.KOTLIN_1_8
    )

    @Test
    fun testMetricFilter() {
        TaskExecutionResults["testKotlin"] = TaskExecutionResult(
            buildMetrics = BuildMetrics(
                buildPerformanceMetrics = BuildPerformanceMetrics().also {
                    it.add(BuildPerformanceMetric.BUNDLE_SIZE)
                    it.add(BuildPerformanceMetric.CACHE_DIRECTORY_SIZE)
                },
                buildTimes = BuildTimes().also {
                    it.addTimeMs(BuildTime.RESTORE_OUTPUT_FROM_BACKUP, 10)
                    it.addTimeMs(BuildTime.IC_ANALYZE_JAR_FILES, 10)
                }
            ),
            taskInfo = TaskExecutionInfo(),
            icLogLines = emptyList()
        )
        val buildOperationRecord = taskRecord(
            BuildMetrics(
                buildPerformanceMetrics = BuildPerformanceMetrics().also {
                    it.add(BuildPerformanceMetric.COMPILE_ITERATION)
                    it.add(BuildPerformanceMetric.CLASSPATH_ENTRY_COUNT)
                },
                buildTimes = BuildTimes().also {
                    it.addTimeMs(BuildTime.STORE_BUILD_INFO, 20)
                    it.addTimeMs(BuildTime.GRADLE_TASK_ACTION, 100)
                }
            )
        )

        val statisticData = prepareData(
            event = taskFinishEvent(),
            projectName = "test",
            uuid = "uuid",
            label = "label",
            kotlinVersion = "version",
            buildOperationRecords = listOf(buildOperationRecord),
            additionalTags = listOf(StatTag.KOTLIN_DEBUG),
            metricsToShow = setOf(
                BuildPerformanceMetric.BUNDLE_SIZE.name,// from TaskExecutionResult
                BuildTime.GRADLE_TASK_ACTION.name,// from buildOperationRecord
                BuildPerformanceMetric.COMPILE_ITERATION.name, //from buildOperationRecord
                BuildTime.IC_CALCULATE_INITIAL_DIRTY_SET.name, //not set
                BuildPerformanceMetric.START_WORKER_EXECUTION.name, //not set
                BuildTime.RESTORE_OUTPUT_FROM_BACKUP.name, //from TaskExecutionResult
            )
        )

        assertNotNull(statisticData)
        assertEquals(2, statisticData.performanceMetrics.size)
        assertTrue(statisticData.performanceMetrics.containsKey(BuildPerformanceMetric.BUNDLE_SIZE))
        assertTrue(statisticData.performanceMetrics.containsKey(BuildPerformanceMetric.COMPILE_ITERATION))
        assertEquals(2, statisticData.buildTimesMetrics.size)
        assertTrue(statisticData.buildTimesMetrics.containsKey(BuildTime.GRADLE_TASK_ACTION))
        assertTrue(statisticData.buildTimesMetrics.containsKey(BuildTime.RESTORE_OUTPUT_FROM_BACKUP))
    }

    @Ignore //temporary ignore flaky test
    @Test
    fun testCalculatedMetrics() {
        val startGradleTask = 20L
        val startTaskAction = 40L
        val callWorker = 100L
        val startWorker = 110L
        val finishGradleTask = System.nanoTime()

        TaskExecutionResults["testKotlin"] = TaskExecutionResult(
            buildMetrics = BuildMetrics(
                buildPerformanceMetrics = BuildPerformanceMetrics().also {
                    it.add(BuildPerformanceMetric.FINISH_KOTLIN_DAEMON_EXECUTION, System.currentTimeMillis())
                    it.add(BuildPerformanceMetric.START_WORKER_EXECUTION, TimeUnit.MILLISECONDS.toNanos(startWorker))
                }
            ),
            taskInfo = TaskExecutionInfo(),
            icLogLines = emptyList()
        )
        val buildOperationRecord = taskRecord(
            BuildMetrics(
                buildPerformanceMetrics = BuildPerformanceMetrics().also {
                    it.add(BuildPerformanceMetric.START_TASK_ACTION_EXECUTION, startTaskAction)
                    it.add(BuildPerformanceMetric.CALL_WORKER, TimeUnit.MILLISECONDS.toNanos(callWorker))
                }
            )
        )

        val statisticData = prepareData(
            event = taskFinishEvent(startTime = startGradleTask, endTime = finishGradleTask),
            projectName = "test",
            uuid = "uuid",
            label = "label",
            kotlinVersion = "version",
            buildOperationRecords = listOf(buildOperationRecord),
            additionalTags = listOf(StatTag.KOTLIN_DEBUG),
        )
        assertNotNull(statisticData)
        assertEquals(startTaskAction - startGradleTask, statisticData.buildTimesMetrics[BuildTime.GRADLE_TASK_PREPARATION])
        assertEquals(1, statisticData.buildTimesMetrics[BuildTime.TASK_FINISH_LISTENER_NOTIFICATION]?.sign)
        assertEquals(startWorker - callWorker, statisticData.buildTimesMetrics[BuildTime.RUN_WORKER_DELAY])
    }

    private fun taskFinishEvent(startTime: Long = 1L, endTime: Long =10L) = object : TaskFinishEvent {
        override fun getEventTime(): Long = System.currentTimeMillis()
        override fun getDisplayName(): String = "some name"
        override fun getDescriptor(): TaskOperationDescriptor = object : TaskOperationDescriptor {
            override fun getName(): String = "task_name"
            override fun getDisplayName(): String = "some name"
            override fun getParent(): OperationDescriptor? = null
            override fun getTaskPath(): String = kotlinTaskPath
            override fun getDependencies(): MutableSet<out OperationDescriptor> = mutableSetOf()
            override fun getOriginPlugin(): PluginIdentifier? = null
        }

        override fun getResult(): TaskOperationResult = object : TaskOperationResult {
            override fun getStartTime(): Long = startTime
            override fun getEndTime(): Long = endTime
        }
    }

}