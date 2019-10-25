/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.performance

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.native.tasks.NativePerformanceReport
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class TaskTimerListener(project: Project) : TaskExecutionListener {
    val tasksTimes = getOrRegisterStorage<String, Double>(project, "tasksTimes")
    val tasksStartedTime = getOrRegisterStorage<String, Long>(project, "tasksStartedTime")
    fun getTime(taskName: String) = tasksTimes[taskName] ?: 0.0

    override fun beforeExecute(task: Task) {
        tasksStartedTime[task.path] = System.nanoTime()
    }

    override fun afterExecute(task: Task, taskState: TaskState) {
        tasksTimes[task.path] = (System.nanoTime() - tasksStartedTime[task.path]!!) / 1000.0
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <K, V> getOrRegisterStorage(project: Project, propertyName: String): ConcurrentHashMap<K, V> =
            project.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java).run {
                if (!has(propertyName)) {
                    set(propertyName, ConcurrentHashMap<K, V>())
                }
                get(propertyName)
            } as ConcurrentHashMap<K, V>
    }

}

open class KotlinPerformancePlugin : Plugin<Project> {
    private lateinit var reportDirectory: File
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

    private fun configureTasks(project: Project, performanceExtension: PerformanceExtension) {
        // Add time listener.
        val timeListener = TaskTimerListener(project)
        project.gradle.addListener(timeListener)
        performanceExtension.trackedBinaries.forEach { binary ->
            project.tasks.create(
                binary.target.disambiguateName(lowerCamelCaseName("perfReport", binary.name)),
                NativePerformanceReport::class.java
            ) {
                it.binary = binary
                it.settings = performanceExtension
                it.timeListener = timeListener
                it.group = TASK_GROUP
                it.description = "Report results of performance measurement for binaries produced by Kotlin/Native compiler."
                binary.linkTask.finalizedBy(it)
            }
        }
    }

    override fun apply(project: Project): Unit = with(project) {
        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val performanceExtension = PerformanceExtension(this)
            reportDirectory = File(project.buildDir, "perfReports")

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