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
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheRequested
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.native.tasks.NativePerformanceReport
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.util.concurrent.ConcurrentHashMap

class TaskTime(val startTime: Long) {
    var duration: Double? = null
}

class TaskTimerListener(project: Project) : TaskExecutionListener {
    val tasksTimes = getOrRegisterStorage<String, TaskTime>(project, "org.jetbrains.kotlin.native.taskTimes")
    fun getTime(taskName: String) = tasksTimes[taskName]?.duration ?: 0.0

    override fun beforeExecute(task: Task) {
        tasksTimes[task.path] = TaskTime(System.nanoTime())
    }

    override fun afterExecute(task: Task, taskState: TaskState) {
        val taskTime = tasksTimes.getValue(task.path)
        taskTime.duration = (System.nanoTime() - taskTime.startTime) / 1000.0
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
        if (!project.isConfigurationCacheRequested) {
            val timeListener = TaskTimerListener(project)
            project.gradle.addListener(timeListener)
            performanceExtension.trackedBinaries.forEach { binary ->
                val perfReport = project.registerTask<NativePerformanceReport>(
                    binary.target.disambiguateName(
                        lowerCamelCaseName(
                            "perfReport",
                            binary.name
                        )
                    )
                ) {
                    it.binary = binary
                    it.settings = performanceExtension
                    it.timeListener = timeListener
                    it.group = TASK_GROUP
                    it.description = "Report performance measurement results for binary '${binary.name}' of target '${binary.target.name}'."
                }
                binary.linkTaskProvider.configure { linkTask -> linkTask.finalizedBy(perfReport) }
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
