/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.gradle.plugin.performance.PerformanceExtension
import org.jetbrains.kotlin.gradle.plugin.performance.TaskTimerListener
import org.jetbrains.kotlin.gradle.plugin.performance.TrackableMetric
import java.io.File

/**
 * The task generates performance report for Kotlin/Native binary.
 */
@DisableCachingByDefault
open class NativePerformanceReport : DefaultTask() {
    @Internal
    lateinit var binary: NativeBinary

    @Internal
    lateinit var timeListener: TaskTimerListener

    @Internal
    val reportDirectory = File(project.buildDir, "perfReports")

    @OutputFile
    val outputFile = File(reportDirectory, "${name}.txt")

    @Internal
    lateinit var settings: PerformanceExtension

    private fun getCompilationResults(tasksPaths: Iterable<String>, success: Boolean): String {
        val status = success && tasksPaths.all { timeListener.tasksTimes.containsKey(it) }
        val time = tasksPaths.map { timeListener.getTime(it) }.sum()
        return "${if (status) "PASSED" else "FAILED"}\nCOMPILE_TIME $time"
    }

    // Get compile task and associated with it other compile tasks.
    private fun getAllExecutedTasks(compilation: KotlinCompilation<*>): List<Task> {
        val tasks = mutableListOf(compilation.compileKotlinTask as Task)
        compilation.associatedCompilations.toList().forEach {
            tasks += getAllExecutedTasks(it)
        }
        return tasks
    }

    private fun folderSize(directory: File): Long {
        var length: Long = 0
        directory.listFiles()?.forEach {
            length += if (it.isFile) it.length() else folderSize(it)
        }
        return length
    }

    private fun compilerFlagsFromBinary(): List<String> {
        val result = mutableListOf<String>()
        if (binary.buildType.optimized) {
            result.add("-opt")
        }
        if (binary.buildType.debuggable) {
            result.add("-g")
        }
        result.addAll(binary.freeCompilerArgs)
        return result
    }

    private fun getPerformanceCompilerOptions() =
        (compilerFlagsFromBinary() + binary.linkTask.compilation.kotlinOptions.freeCompilerArgs)
            .filter { it in listOf("-g", "-opt", "-Xg0") }.map { "\"$it\"" }

    @TaskAction
    fun generate() {
        val compileTasks = if (settings.includeAssociatedTasks)
            getAllExecutedTasks(binary.linkTask.compilation)
        else
            listOf(binary.linkTask.compilation.compileKotlinTask)
        val allExecutedTasks = listOf(binary.linkTask) + compileTasks
        val upToDateTasks = allExecutedTasks.filter { it.state.upToDate }.map { it.name }
        if (upToDateTasks.isNotEmpty()) {
            if (outputFile.exists()) {
                project.delete(outputFile.absolutePath)
            }
            project.logger.warn(
                "Next compile tasks which are needed for time measurement are upToDate" +
                        " and weren't executed:\n${upToDateTasks.joinToString("\n", "- ")}"
            )
            return
        }
        val successStatus = allExecutedTasks.all { it.state.failure == null }
        // Get code size metric.
        var codeSize: String? = null
        if (TrackableMetric.CODE_SIZE in settings.metrics) {
            codeSize = binary.outputFile.let {
                if (it.exists()) {
                    val size = if (it.isDirectory) folderSize(it) else it.length()
                    "CODE_SIZE $size"
                } else null
            }
        }
        // Get compile time.
        var compileTime: String? = null
        if (TrackableMetric.COMPILE_TIME in settings.metrics) {
            compileTime = getCompilationResults(
                allExecutedTasks.map { it.path },
                successStatus
            )
        }

        // Create report.
        if (!reportDirectory.exists()) {
            project.mkdir(reportDirectory.absolutePath)
        }
        val name = settings.binaryNamesForReport[binary]!!
        outputFile.writeText(name)
        outputFile.appendText("\nOPTIONS ${getPerformanceCompilerOptions()}")
        if (compileTime != null) {
            outputFile.appendText("\n$compileTime")
        }
        if (codeSize != null) {
            outputFile.appendText("\n$codeSize")
        }
    }
}