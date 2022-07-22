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
import org.jetbrains.kotlin.build.report.metrics.SizeMetricType
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.utils.formatSize
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.util.*
import kotlin.system.measureTimeMillis

class BuildScanStatisticsListener(
    private val buildScan: BuildScanExtension,
    val projectName: String,
    val label: String?,
    val kotlinVersion: String,
    val buildUuid: String
) : OperationCompletionListener, AutoCloseable {
    companion object {
        const val lengthLimit = 100_000
        const val customValuesLimit = 950 //git plugin and others can add custom values as well
    }

    private val tags = LinkedHashSet<String>()
    private val log = Logging.getLogger(this.javaClass)
    private var customValues = 0 // doesn't need to be thread-safe

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
                KotlinBuildStatListener.prepareData(event, projectName, buildUuid, label, kotlinVersion)
            }
            log.debug("Collect data takes $collectDataDuration: $compileStatData")

            compileStatData?.also {
                report(it)
            }
        }
    }

    override fun close() {
    }

    fun report(data: CompileStatisticsData) {
        val elapsedTime = measureTimeMillis {
            if (!tags.contains(buildUuid)) {
                tags.add(buildUuid)
            }
            data.label?.takeIf { !tags.contains(it) }?.also {
                buildScan.tag(it)
                tags.add(it)
            }
            data.tags
                .filter { !tags.contains(it) }
                .forEach {
                    buildScan.tag(it)
                    tags.add(it)
                }

            if (customValues < customValuesLimit) {
                readableString(data).forEach {
                    if (customValues < customValuesLimit) {
                        buildScan.value(data.taskName, it)
                        customValues++
                    } else {
                        log.debug("Statistic data for ${data.taskName} was cut due to custom values limit.")
                    }
                }
            } else {
                log.debug("Can't add any more custom values into build scan.")
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
                SizeMetricType.BYTES -> "${key.readableString}: ${formatSize(value)}"
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
}
