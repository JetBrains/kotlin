/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.performance

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.addExtension
import java.io.File

class TaskTimerListener : TaskExecutionListener {
    companion object {
        val tasksTimes = mutableMapOf<String, Double>()
        fun getTime(taskName: String) = tasksTimes[taskName] ?: 0.0
    }

    private var startTime = System.nanoTime()

    override fun beforeExecute(task: Task) {
        startTime = System.nanoTime()
    }

    override fun afterExecute(task: Task, taskState: TaskState) {
        tasksTimes[task.name] = (System.nanoTime() - startTime) / 1000.0
    }
}

open class KotlinPerformancePlugin : Plugin<Project> {
    private fun checkSettings(project: Project, performanceExtension: PerformanceExtension): Boolean {
        var result = true
        if (performanceExtension.metrics.isEmpty()) {
            project.logger.warn("There is no tracked metrics. Please, provide metrics in settings of $EXTENSION_NAME plugin.")
            result = false
        }
        // Find binaries with trackable performance.
        if (performanceExtension.trackedBinaries.isEmpty()) {
            project.logger.warn("There is no tracked binaries. Please, provide binaries in settings of $EXTENSION_NAME plugin.")
            result = false
        }
        return result
    }

    private fun getCompilationResults(tasksNames: Iterable<String>, success: Boolean): String {
        var time = 0.0
        var status = true
        tasksNames.forEach {
            time += TaskTimerListener.getTime(it)
            status = tasksNames.map { TaskTimerListener.tasksTimes.containsKey(it) }.reduce { a, b -> a && b }
            status = status && success
        }
        return "${if (status) "PASSED" else "FAILED"}\nCOMPILE_TIME $time"
    }


    // Get compile task and associated with it other compile tasks.
    private fun getAllExecutedTasks(compilation: KotlinCompilation<*>): List<Task> {
        val tasks = mutableListOf(compilation.compileKotlinTask as Task)
        compilation.associateWith.forEach {
            tasks += getAllExecutedTasks(it)
        }
        return tasks
    }

    private fun configureTasks(project: Project, performanceExtension: PerformanceExtension) {
        // Add time listener.
        project.gradle.addListener(TaskTimerListener())
        performanceExtension.trackedBinaries.forEach { binary ->
            project.tasks.create(
                "perf${binary.buildType.toString().toLowerCase().capitalize()}" +
                        "Report${binary.konanTarget.name.capitalize()}"
            ) {
                it.group = TASK_GROUP
                it.description = "Report results of performance measurement for Kotlin/Native targets."
                it.doLast {
                    val compileTasks = getAllExecutedTasks(binary.linkTask.compilation)
                    val successStatus = compileTasks.map { it.state.failure == null }.reduce { acc, it -> acc && it }
                    // Get code size metric.
                    var codeSize: String? = null
                    if (TrackableMetric.CODE_SIZE in performanceExtension.metrics) {
                        codeSize = binary.outputFile.let {
                            if (it.exists()) "CODE_SIZE ${it.length()}" else null
                        }
                    }
                    // Get compile time.
                    var compileTime: String? = null
                    if (TrackableMetric.COMPILE_TIME in performanceExtension.metrics) {
                        compileTime = getCompilationResults(
                            listOf(binary.linkTaskName) + compileTasks.map { it.name },
                            successStatus
                        )
                    }

                    // Create report.
                    val reportFile = File(project.buildDir, "${it.name}.txt")
                    val name = performanceExtension.binaryNamesForReport.getOrDefault(binary, binary.name)
                    reportFile.writeText(name)
                    if (compileTime != null) {
                        reportFile.appendText("\n$compileTime")
                    }
                    if (codeSize != null) {
                        reportFile.appendText("\n$codeSize")
                    }
                }
                binary.linkTask.finalizedBy(it)
            }
        }
    }

    override fun apply(project: Project): Unit = with(project) {
        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val performanceExtension = PerformanceExtension(this)

            kotlinExtension.addExtension(EXTENSION_NAME, performanceExtension)
            afterEvaluate {
                if (checkSettings(project, performanceExtension)) {
                    configureTasks(project, performanceExtension)
                }
            }
        }
    }

    companion object {
        const val EXTENSION_NAME = "performance"
        const val TASK_GROUP = "Kotlin/Native Performance"
    }
}