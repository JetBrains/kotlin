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
import org.jetbrains.kotlin.build.report.statistics.StatTag
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
        val buildOperationRecord =
            taskRecord(BuildMetrics(buildAttributes = BuildAttributes().also { it.add(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND) }))
        val statisticData = prepareData(
            event = taskFinishEvent(),
            projectName = "test",
            uuid = "uuid",
            label = "label",
            kotlinVersion = "version",
            onlyKotlinTask = true,
            buildOperationRecord = buildOperationRecord,
            additionalTags = setOf(StatTag.KOTLIN_DEBUG)
        )

        assertNotNull(statisticData)
        assertTrue(statisticData.getTags().contains(StatTag.KOTLIN_DEBUG))
        assertTrue(statisticData.getTags().contains(StatTag.NON_INCREMENTAL))
        assertTrue(statisticData.getTags().contains(StatTag.KOTLIN_1))
    }

    private fun taskRecord(buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>) = TaskRecord(
        path = kotlinTaskPath,
        classFqName = "org.jetbrains.kotlin.TestTask",
        startTimeMs = 10,
        totalTimeMs = 20,
        buildMetrics = buildMetrics,
        didWork = true,
        skipMessage = null,
        icLogLines = emptyList(),
        kotlinLanguageVersion = KotlinVersion.KOTLIN_1_8,
        changedFiles = null,
        compilerArguments = emptyArray(),
        statTags = emptySet()
    )

    @Test
    fun testMetricFilter() {
        val buildOperationRecord = taskRecord(
            BuildMetrics(
                buildPerformanceMetrics = BuildPerformanceMetrics<GradleBuildPerformanceMetric>().also {
                    it.add(GradleBuildPerformanceMetric.COMPILE_ITERATION)
                    it.add(GradleBuildPerformanceMetric.CLASSPATH_ENTRY_COUNT)
                    it.add(GradleBuildPerformanceMetric.BUNDLE_SIZE)
                    it.add(GradleBuildPerformanceMetric.CACHE_DIRECTORY_SIZE)
                },
                buildTimes = BuildTimes<GradleBuildTime>().also {
                    it.addTimeMs(GradleBuildTime.STORE_BUILD_INFO, 20)
                    it.addTimeMs(GradleBuildTime.GRADLE_TASK_ACTION, 100)
                    it.addTimeMs(GradleBuildTime.RESTORE_OUTPUT_FROM_BACKUP, 10)
                    it.addTimeMs(GradleBuildTime.IC_ANALYZE_JAR_FILES, 10)
                }
            )
        )

        val statisticData = prepareData(
            event = taskFinishEvent(),
            projectName = "test",
            uuid = "uuid",
            label = "label",
            kotlinVersion = "version",
            buildOperationRecord = buildOperationRecord,
            onlyKotlinTask = true,
            additionalTags = setOf(StatTag.KOTLIN_DEBUG),
            metricsToShow = setOf(
                GradleBuildPerformanceMetric.BUNDLE_SIZE.name,// from TaskExecutionResult
                GradleBuildTime.GRADLE_TASK_ACTION.name,// from buildOperationRecord
                GradleBuildPerformanceMetric.COMPILE_ITERATION.name, //from buildOperationRecord
                GradleBuildTime.IC_CALCULATE_INITIAL_DIRTY_SET.name, //not set
                GradleBuildPerformanceMetric.START_WORKER_EXECUTION.name, //not set
                GradleBuildTime.RESTORE_OUTPUT_FROM_BACKUP.name, //from TaskExecutionResult
            )
        )

        assertNotNull(statisticData)
        assertEquals(2, statisticData.getPerformanceMetrics().size)
        assertTrue(statisticData.getPerformanceMetrics().containsKey(GradleBuildPerformanceMetric.BUNDLE_SIZE))
        assertTrue(statisticData.getPerformanceMetrics().containsKey(GradleBuildPerformanceMetric.COMPILE_ITERATION))
        assertEquals(2, statisticData.getBuildTimesMetrics().size)
        assertTrue(statisticData.getBuildTimesMetrics().containsKey(GradleBuildTime.GRADLE_TASK_ACTION))
        assertTrue(statisticData.getBuildTimesMetrics().containsKey(GradleBuildTime.RESTORE_OUTPUT_FROM_BACKUP))
    }

    @Ignore //temporary ignore flaky test
    @Test
    fun testCalculatedMetrics() {
        val startGradleTask = 20L
        val startTaskAction = 40L
        val callWorker = 100L
        val startWorker = 110L
        val finishGradleTask = System.nanoTime()

        val buildOperationRecord = taskRecord(
            BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>(
                buildPerformanceMetrics = BuildPerformanceMetrics<GradleBuildPerformanceMetric>().also {
                    it.add(GradleBuildPerformanceMetric.FINISH_KOTLIN_DAEMON_EXECUTION, System.currentTimeMillis())
                    it.add(GradleBuildPerformanceMetric.START_WORKER_EXECUTION, TimeUnit.MILLISECONDS.toNanos(startWorker))
                    it.add(GradleBuildPerformanceMetric.START_TASK_ACTION_EXECUTION, startTaskAction)
                    it.add(GradleBuildPerformanceMetric.CALL_WORKER, TimeUnit.MILLISECONDS.toNanos(callWorker))
                }
            )
        )

        val statisticData = prepareData(
            event = taskFinishEvent(startTime = startGradleTask, endTime = finishGradleTask),
            projectName = "test",
            uuid = "uuid",
            label = "label",
            kotlinVersion = "version",
            buildOperationRecord = buildOperationRecord,
            onlyKotlinTask = true,
            additionalTags = setOf(StatTag.KOTLIN_DEBUG),
        )
        assertNotNull(statisticData)
        assertEquals(startTaskAction - startGradleTask, statisticData.getBuildTimesMetrics()[GradleBuildTime.GRADLE_TASK_PREPARATION])
        assertEquals(1, statisticData.getBuildTimesMetrics()[GradleBuildTime.TASK_FINISH_LISTENER_NOTIFICATION]?.sign)
        assertEquals(startWorker - callWorker, statisticData.getBuildTimesMetrics()[GradleBuildTime.RUN_WORKER_DELAY])
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