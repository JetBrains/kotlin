/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.api.logging.Logging
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.metrics.BuildMetricType
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet
import kotlin.system.measureTimeMillis

class BuildScanStatisticsListener(
    private val buildScan: BuildScanExtension,
    val projectName: String,
    val label: String?,
    val kotlinVersion: String,
) : OperationCompletionListener, AutoCloseable {
    companion object {
        const val kbSize = 1024
        const val mbSize = kbSize * kbSize
        const val gbSize = kbSize * mbSize
        const val lengthLimit = 100_000
    }

    private val tags = LinkedHashSet<String>()
    private val log = Logging.getLogger(this.javaClass)
    private val buildUuid: String = UUID.randomUUID().toString()

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
                KotlinBuildStatListener.prepareData(event, projectName, buildUuid, label, kotlinVersion)
            }
            log.debug("Collect data takes $collectDataDuration: $compileStatData")

            compileStatData?.also {
                val reportDataDuration = measureTimeMillis {
                    report(it)
                }
                log.debug("Report data takes $reportDataDuration: $compileStatData")
            }
        }
    }

    override fun close() {
    }

    fun report(data: CompileStatisticsData) {
        val elapsedTime = measureTimeMillis {

            readableString(data).forEach { buildScan.value(data.taskName, it) }

            data.tags
                .filter { !tags.contains(it) }
                .forEach {
                    buildScan.tag(it)
                    tags.add(it)
                }
        }
        log.debug("Report statistic to build scan takes $elapsedTime ms")
    }

    private fun readableString(data: CompileStatisticsData): List<String> {
        val readableString = StringBuilder()
        if (data.nonIncrementalAttributes.isEmpty()) {
            readableString.append("Incremental build; ")
            data.changes.joinTo(readableString, prefix = "Changes: [", postfix = "]; ") { it.substringAfterLast(File.separator) }
        } else {
            data.nonIncrementalAttributes.joinTo(readableString, prefix = "Non incremental build because: [", postfix = "]; ") { it.readableString }
        }

        val timeData =
            data.buildTimesMetrics.map { (key, value) -> "${key.readableString}: ${value}ms" } //sometimes it is better to have separate variable to be able debug
        val perfData = data.performanceMetrics.map { (key, value) ->
            when (key.type) {
                BuildMetricType.FILE_SIZE -> "${key.readableString}: ${readableFileLength(value)}"
                else -> "${key.readableString}: $value}"
            }
        }
        timeData.union(perfData).joinTo(readableString, ",", "Performance: [", "]")

        return splitStringIfNeed(readableString.toString(), lengthLimit)
    }

    private fun splitStringIfNeed(str: String, lengthLimit: Int): List<String> {
        val splattedString = ArrayList<String>()
        var tempStr = str
        while (tempStr.length > lengthLimit) {
            val subSequence = tempStr.substring(lengthLimit)
            var index = subSequence.lastIndexOf(';')
            if (index == -1) {
                index = subSequence.lastIndexOf(',')
                if (index == -1) {
                    index = lengthLimit
                }
            }
            splattedString.add(tempStr.substring(index))
            tempStr = tempStr.substring(index)

        }
        splattedString.add(tempStr)
        return splattedString
    }

    private fun readableFileLength(length: Long): String =
        when {
            length / gbSize > 0 -> "${length / gbSize} GB"
            length / mbSize > 0 -> "${length / mbSize} MB"
            length / kbSize > 0 -> "${length / kbSize} KB"
            else -> "$length B"
        }
}
