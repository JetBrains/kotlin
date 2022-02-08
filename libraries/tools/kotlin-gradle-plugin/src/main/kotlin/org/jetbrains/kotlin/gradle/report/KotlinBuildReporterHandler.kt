/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File
import kotlin.math.max

class KotlinBuildReporterHandler {
    fun buildFinished(
        gradle: Gradle,
        perfReportFile: File,
        kotlinTaskTimeNs: Map<String, Long>,
        allTasksTimeNs: Long,
        failure: Throwable? = null
    ) {
        val logger = gradle.rootProject.logger
        try {
            perfReportFile.writeText(buildInfo(gradle, failure) + taskOverview(kotlinTaskTimeNs, allTasksTimeNs))
            logger.lifecycle("Kotlin build report is written to ${perfReportFile.canonicalPath}")
        } catch (e: Throwable) {
            logger.error("Could not write Kotlin build report to ${perfReportFile.canonicalPath}", e)
        }
    }

    private fun buildInfo(gradle: Gradle, failure: Throwable?): String {
        val startParams = arrayListOf<String>()
        gradle.startParameter.apply {
            startParams.add("tasks = ${taskRequests.joinToString { it.args.toString() }}")
            startParams.add("excluded tasks = $excludedTaskNames")
            startParams.add("current dir = $currentDir")
            startParams.add("project properties args = $projectProperties")
            startParams.add("system properties args = $systemPropertiesArgs")
        }

        return buildString {
            appendLine("Gradle start parameters:")
            startParams.forEach {
                appendLine("  $it")
            }
            if (failure != null) {
                appendLine("Build failed: ${failure}")
            }
            appendLine()
        }
    }

    internal fun taskOverview(kotlinTaskTimeNs: Map<String, Long>, allTasksTimeNs: Long): String {
        if (kotlinTaskTimeNs.isEmpty()) return buildString { appendLine("No Kotlin task was run") }

        val sb = StringBuilder()
        val kotlinTotalTimeNs = kotlinTaskTimeNs.values.sum()
        val ktTaskPercent = (kotlinTotalTimeNs.toDouble() / allTasksTimeNs * 100).asString(1)

        sb.appendLine("Total time for Kotlin tasks: ${formatTime(kotlinTotalTimeNs)} ($ktTaskPercent % of all tasks time)")

        val table = TextTable("Time", "% of Kotlin time", "Task")
        kotlinTaskTimeNs.entries
            .sortedByDescending { (_, timeNs) -> timeNs }
            .forEach { (taskPath, timeNs) ->
                val percent = (timeNs.toDouble() / kotlinTotalTimeNs * 100).asString(1)
                table.addRow(formatTime(timeNs), "$percent %", taskPath)
            }
        table.printTo(sb)
        sb.appendLine()
        return sb.toString()
    }

    internal class TextTable(vararg columnNames: String) {
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
                sb.appendLine()
                for ((i, col) in row.withIndex()) {
                    if (i > 0) sb.append("|")

                    sb.append(col.padEnd(maxLengths[i], ' '))
                }
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    val seconds = ms.toDouble() / 1_000
    return seconds.asString(2) + " s"
}

internal fun Double.asString(decPoints: Int): String =
    String.format("%.${decPoints}f", this)
