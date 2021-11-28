/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import org.jetbrains.kotlin.gradle.report.data.TaskExecutionData
import java.util.*

internal class BuildDataRecorder(
    private val gradle: Gradle,
    private val buildDataProcessors: Iterable<BuildExecutionDataProcessor>
) : BuildAdapter(), TaskExecutionListener {
    private val taskRecords = HashMap<Task, TaskRecord>()

    @Synchronized
    override fun beforeExecute(task: Task) {
        val startNs = System.nanoTime()
        taskRecords[task] = TaskRecord(task, startNs)
    }

    @Synchronized
    override fun afterExecute(task: Task, state: TaskState) {
        taskRecords[task]?.apply { processResult(state, TaskExecutionResults[task.path]) }
    }

    override fun buildFinished(result: BuildResult) {
        val startParams = arrayListOf<String>()
        gradle.startParameter.apply {
            startParams.add("tasks = ${taskRequests.joinToString { it.args.toString() }}")
            startParams.add("excluded tasks = $excludedTaskNames")
            startParams.add("current dir = $currentDir")
            startParams.add("project properties args = $projectProperties")
            startParams.add("system properties args = $systemPropertiesArgs")
        }
        val buildData = BuildExecutionData(
            startParameters = startParams,
            failure = result.failure,
            taskExecutionData = taskRecords.values.sortedBy { it.startNs }
        )

        buildDataProcessors.forEach { it.process(buildData) }
    }
}

private class TaskRecord(
    override val task: Task,
    override val startNs: Long
) : TaskExecutionData {
    private var myEndNs: Long = 0
    private lateinit var myTaskState: TaskState
    private var myIcLogLines: List<String> = emptyList()
    private val myBuildMetrics = BuildMetrics()

    override val isKotlinTask by lazy {
        task.javaClass.name.startsWith("org.jetbrains.kotlin")
    }

    override val endNs: Long
        get() = myEndNs

    override val totalTimeMs: Long
        get() = (endNs - startNs) / 1_000_000

    override val resultState: TaskState
        get() = myTaskState

    override val icLogLines: List<String>
        get() = myIcLogLines

    override val buildMetrics: BuildMetrics
        get() = myBuildMetrics

    fun processResult(state: TaskState, executionResult: TaskExecutionResult?) {
        myEndNs = System.nanoTime()
        myTaskState = state
        myBuildMetrics.buildTimes.add(BuildTime.GRADLE_TASK, totalTimeMs)

        if (executionResult != null) {
            myBuildMetrics.addAll(executionResult.buildMetrics)
            myIcLogLines = executionResult.icLogLines
        }

        if (task is TaskWithLocalState) {
            myBuildMetrics.addAll(task.metrics.get().getMetrics())
        }
    }
}
