/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

internal fun configureBuildReporter(gradle: Gradle, log: Logger) {
    val rootProject = gradle.rootProject
    val properties = PropertiesProvider(rootProject)

    if (!properties.buildReportEnabled) return

    val perfLogDir = properties.buildReportDir
        ?: rootProject.buildDir.resolve("reports/kotlin-build").apply { mkdirs() }

    if (perfLogDir.isFile) {
        log.error("Kotlin build report cannot be created: '$perfLogDir' is a file")
        return
    }

    perfLogDir.mkdirs()
    val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)

    val perfReportFile = perfLogDir.resolve("${gradle.rootProject.name}-build-$ts.txt")
    val reporter = KotlinBuildReporter(gradle, perfReportFile)
    gradle.addBuildListener(reporter)

    val buildReportMode = if (properties.buildReportVerbose) BuildReportMode.VERBOSE else BuildReportMode.SIMPLE

    gradle.taskGraph.whenReady { graph ->
        graph.allTasks.asSequence()
            .filterIsInstance<AbstractKotlinCompile<*>>()
            .forEach { it.buildReportMode = buildReportMode }
    }

    log.kotlinDebug { "Configured Kotlin build reporter" }
}

internal class KotlinBuildReporter(
    private val gradle: Gradle,
    private val perfReportFile: File
) : BuildAdapter(), TaskExecutionListener {
    init {
        val dir = perfReportFile.parentFile
        check(dir.isDirectory) { "$dir does not exist or is a file" }
        check(!perfReportFile.isFile) { "Build report log file $perfReportFile exists already" }
    }

    private val taskStartNs = HashMap<Task, Long>()
    private val kotlinTaskTimeNs = HashMap<Task, Long>()
    private val tasksSb = StringBuilder()

    @Volatile
    private var allTasksTimeNs: Long = 0L

    @Synchronized
    override fun beforeExecute(task: Task) {
        taskStartNs[task] = System.nanoTime()
    }

    @Synchronized
    override fun afterExecute(task: Task, state: TaskState) {
        val startNs = taskStartNs[task] ?: return

        val endNs = System.nanoTime()
        val timeNs = endNs - startNs
        allTasksTimeNs += timeNs

        if (!task.javaClass.name.startsWith("org.jetbrains.kotlin")) {
            return
        }

        kotlinTaskTimeNs[task] = timeNs
        tasksSb.appendln()

        val skipMessage = state.skipMessage
        if (skipMessage != null) {
            tasksSb.appendln("$task was skipped: $skipMessage")
        } else {
            tasksSb.appendln("$task finished in ${formatTime(timeNs)}")
        }

        val path = task.path
        val executionResult = TaskExecutionResults[path]
        if (executionResult != null) {
            tasksSb.appendln("Execution strategy: ${executionResult.executionStrategy}")

            executionResult.icLogLines?.let { lines ->
                tasksSb.appendln("Compilation log for $task:")
                lines.forEach { tasksSb.appendln("  $it") }
            }
        }
    }

    @Synchronized
    override fun buildFinished(result: BuildResult) {
        val logger = result.gradle?.rootProject?.logger
        try {
            perfReportFile.writeText(buildInfo(result) + taskOverview() + tasksSb.toString())
            logger?.lifecycle("Kotlin build report is written to ${perfReportFile.canonicalPath}")
        } catch (e: Throwable) {
            logger?.error("Could not write Kotlin build report to ${perfReportFile.canonicalPath}", e)
        }
    }

    private fun buildInfo(result: BuildResult): String {
        val startParams = arrayListOf<String>()
        gradle.startParameter.apply {
            startParams.add("tasks = ${taskRequests.joinToString { it.args.toString() }}")
            startParams.add("excluded tasks = $excludedTaskNames")
            startParams.add("current dir = $currentDir")
            startParams.add("project properties args = $projectProperties")
            startParams.add("system properties args = $systemPropertiesArgs")
        }

        return buildString {
            appendln("Gradle start parameters:")
            startParams.forEach {
                appendln("  $it")
            }
            if (result.failure != null) {
                appendln("Build failed: ${result.failure}")
            }
            appendln()
        }
    }

    private fun taskOverview(): String {
        if (kotlinTaskTimeNs.isEmpty()) return buildString { appendln("No Kotlin task was run") }

        val sb = StringBuilder()
        val kotlinTotalTimeNs = kotlinTaskTimeNs.values.sum()
        val ktTaskPercent = (kotlinTotalTimeNs.toDouble() / allTasksTimeNs * 100).asString(1)

        sb.appendln("Total time for Kotlin tasks: ${formatTime(kotlinTotalTimeNs)} ($ktTaskPercent % of all tasks time)")

        val table = TextTable("Time", "% of Kotlin time", "Task")
        kotlinTaskTimeNs.entries
            .sortedByDescending { (_, timeNs) -> timeNs }
            .forEach { (task, timeNs) ->
                val percent = (timeNs.toDouble() / kotlinTotalTimeNs * 100).asString(1)
                table.addRow(formatTime(timeNs), "$percent %", task.path)
            }
        table.printTo(sb)
        sb.appendln()
        return sb.toString()
    }

    private fun formatTime(ns: Long): String {
        val seconds = ns.toDouble() / 1_000_000_000
        return seconds.asString(2) + " s"
    }

    private fun Double.asString(decPoints: Int): String =
        String.format("%.${decPoints}f", this)

    private class TextTable(vararg columnNames: String) {
        private val rows = ArrayList<List<String>>()
        private val columnsCount = columnNames.size
        private val maxLengths = IntArray(columnsCount) { columnNames[it].length }

        init {
            rows.add(columnNames.toList())
        }

        fun addRow(vararg row: String) {
            check(row.size == columnsCount) { "Row size ${row.size} differs from columns count $columnsCount" }
            rows.add(row.toList())

            for ((i, col) in row.withIndex()) {
                maxLengths[i] = max(maxLengths[i], col.length)
            }
        }

        fun printTo(sb: StringBuilder) {
            for (row in rows) {
                sb.appendln()
                for ((i, col) in row.withIndex()) {
                    if (i > 0) sb.append("|")

                    sb.append(col.padEnd(maxLengths[i], ' '))
                }
            }
        }
    }
}

