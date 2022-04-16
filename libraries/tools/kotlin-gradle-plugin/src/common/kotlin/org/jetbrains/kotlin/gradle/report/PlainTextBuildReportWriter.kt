/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionDataProcessor
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.gradle.utils.Printer
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

internal class PlainTextBuildReportWriterDataProcessor(
    val reportingSettings: FileReportSettings,
    val rootProjectName: String
) : BuildExecutionDataProcessor, Serializable {
    override fun process(build: BuildExecutionData, log: Logger) {
        val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
        val reportFile = reportingSettings.buildReportDir.resolve("${rootProjectName}-build-$ts.txt")

        PlainTextBuildReportWriter(
            outputFile = reportFile,
            printMetrics = reportingSettings.includeMetricsInReport
        ).process(build, log)
    }
}

internal class PlainTextBuildReportWriter(
    private val outputFile: File,
    private val printMetrics: Boolean
) : Serializable {

    private lateinit var p: Printer

    fun process(build: BuildExecutionData, log: Logger) {
        try {
            outputFile.parentFile.mkdirs()
            if (!(outputFile.parentFile.exists() && outputFile.parentFile.isDirectory)) {
                log.error("Kotlin build report cannot be created: '$outputFile.parentFile' is a file or do not have permissions to create")
                return
            }

            outputFile.bufferedWriter().use { writer ->
                p = Printer(writer)
                printBuildReport(build)
            }

            log.lifecycle("Kotlin build report is written to ${outputFile.canonicalPath}")
        } catch (e: Exception) {
            log.error("Could not write Kotlin build report to ${outputFile.canonicalPath}", e)
        }
    }

    private fun printBuildReport(build: BuildExecutionData) {
        // NOTE: BuildExecutionData / BuildOperationRecord contains data for both tasks and transforms.
        // Where possible, we still use the term "tasks" because saying "tasks/transforms" is a bit verbose and "build operations" may sound
        // a bit unfamiliar.
        // TODO: If it is confusing, consider renaming "tasks" to "build operations" in this class.
        printBuildInfo(build)
        if (printMetrics) {
            printMetrics(build.aggregatedMetrics)
            p.println()
        }
        printTaskOverview(build)
        printTasksLog(build)
    }

    private fun printBuildInfo(build: BuildExecutionData) {
        p.withIndent("Gradle start parameters:") {
            build.startParameters.forEach { p.println(it) }
        }
        p.println()

        if (build.failureMessages.isNotEmpty()) {
            p.println("Build failed: ${build.failureMessages}")
            p.println()
        }
    }

    private fun printMetrics(buildMetrics: BuildMetrics) {
        printBuildTimes(buildMetrics.buildTimes)
        printBuildPerformanceMetrics(buildMetrics.buildPerformanceMetrics)
        printBuildAttributes(buildMetrics.buildAttributes)
    }

    private fun printBuildTimes(buildTimes: BuildTimes) {
        val buildTimesMs = buildTimes.asMapMs()

        if (buildTimesMs.isEmpty()) return

        p.println("Time metrics:")
        p.withIndent {
            val visitedBuildTimes = HashSet<BuildTime>()
            fun printBuildTime(buildTime: BuildTime) {
                if (!visitedBuildTimes.add(buildTime)) return

                val timeMs = buildTimesMs[buildTime]
                if (timeMs != null) {
                    p.println("${buildTime.readableString}: ${formatTime(timeMs)}")
                    p.withIndent {
                        BuildTime.children[buildTime]?.forEach { printBuildTime(it) }
                    }
                } else {
                    //Skip formatting if parent metric does not set
                    BuildTime.children[buildTime]?.forEach { printBuildTime(it) }
                }
            }

            for (buildTime in BuildTime.values()) {
                if (buildTime.parent != null) continue

                printBuildTime(buildTime)
            }
        }
    }

    private fun printBuildPerformanceMetrics(buildMetrics: BuildPerformanceMetrics) {
        val allBuildMetrics = buildMetrics.asMap()
        if (allBuildMetrics.isEmpty()) return

        p.withIndent("Size metrics:") {
            for (metric in BuildPerformanceMetric.values()) {
                allBuildMetrics[metric]?.let { printSizeMetric(metric, it) }
            }
        }
    }

    private fun printSizeMetric(sizeMetric: BuildPerformanceMetric, value: Long) {
        fun BuildPerformanceMetric.numberOfAncestors(): Int {
            var count = 0
            var parent: BuildPerformanceMetric? = parent
            while (parent != null) {
                count++
                parent = parent.parent
            }
            return count
        }

        val indentLevel = sizeMetric.numberOfAncestors()

        repeat(indentLevel) { p.pushIndent() }
        when (sizeMetric.type) {
            SizeMetricType.BYTES -> p.println("${sizeMetric.readableString}: ${formatSize(value)}")
            SizeMetricType.NUMBER -> p.println("${sizeMetric.readableString}: $value")
        }
        repeat(indentLevel) { p.popIndent() }
    }

    private fun printBuildAttributes(buildAttributes: BuildAttributes) {
        val allAttributes = buildAttributes.asMap()
        if (allAttributes.isEmpty()) return

        p.withIndent("Build attributes:") {
            val attributesByKind = allAttributes.entries.groupBy { it.key.kind }.toSortedMap()
            for ((kind, attributesCounts) in attributesByKind) {
                printMap(p, kind.name, attributesCounts.map { (k, v) -> k.readableString to v }.toMap())
            }
        }
    }

    private fun printTaskOverview(build: BuildExecutionData) {
        var allTasksTimeMs = 0L
        var kotlinTotalTimeMs = 0L
        val kotlinTasks = ArrayList<BuildOperationRecord>()

        for (task in build.buildOperationRecord) {
            val taskTimeMs = task.totalTimeMs
            allTasksTimeMs += taskTimeMs

            if (task.isFromKotlinPlugin) {
                kotlinTotalTimeMs += taskTimeMs
                kotlinTasks.add(task)
            }
        }

        if (kotlinTasks.isEmpty()) {
            p.println("No Kotlin task was run")
            return
        }

        val ktTaskPercent = (kotlinTotalTimeMs.toDouble() / allTasksTimeMs * 100).asString(1)
        p.println("Total time for Kotlin tasks: ${formatTime(kotlinTotalTimeMs)} ($ktTaskPercent % of all tasks time)")

        val table = TextTable("Time", "% of Kotlin time", "Task")
        for (task in kotlinTasks.sortedWith(compareBy({ -it.totalTimeMs }, { it.startTimeMs }))) {
            val timeMs = task.totalTimeMs
            val percent = (timeMs.toDouble() / kotlinTotalTimeMs * 100).asString(1)
            table.addRow(formatTime(timeMs), "$percent %", task.path)
        }
        table.printTo(p)
        p.println()
    }

    private fun printTasksLog(build: BuildExecutionData) {
        for (task in build.buildOperationRecord.sortedWith(compareBy({ -it.totalTimeMs }, { it.startTimeMs }))) {
            printTaskLog(task)
            p.println()
        }
    }

    private fun printTaskLog(task: BuildOperationRecord) {
        val skipMessage = task.skipMessage
        if (skipMessage != null) {
            p.println("Task '${task.path}' was skipped: $skipMessage")
        } else {
            p.println("Task '${task.path}' finished in ${formatTime(task.totalTimeMs)}")
        }

        if (task.icLogLines.isNotEmpty()) {
            p.withIndent("Compilation log for task '${task.path}':") {
                task.icLogLines.forEach { p.println(it) }
            }
        }

        if (printMetrics) {
            printMetrics(task.buildMetrics)
        }
    }
}

private fun printMap(p: Printer, name: String, mapping: Map<String, Int>) {
    if (mapping.isEmpty()) return

    if (mapping.size == 1) {
        p.println("$name: ${mapping.keys.single()}")
        return
    }

    p.withIndent("$name:") {
        val sortedEnumMap = mapping.toSortedMap()
        for ((k, v) in sortedEnumMap) {
            p.println("$k($v)")
        }
    }
}

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

    fun printTo(p: Printer) {
        for (row in rows) {
            val rowStr = row.withIndex().joinToString("|") { (i, col) -> col.padEnd(maxLengths[i], ' ') }
            p.println(rowStr)
        }
    }
}
