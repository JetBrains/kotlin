/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.konan.file.File
import kotlin.system.measureTimeMillis

class ReportStatisticsToBuildScan(
    private val buildScan: BuildScanExtension
) : ReportStatistics {
    companion object {
        const val kbSize = 1024
        const val mbSize = kbSize * kbSize
        const val gbSize = kbSize * mbSize
        const val lengthLimit = 20
    }

    private val tags = LinkedHashSet<String>()
    private val log = Logging.getLogger(this.javaClass)

    override fun report(data: CompileStatData) {
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

    private fun readableString(data: CompileStatData): List<String> {
        val nonIncrementalReasons = data.nonIncrementalAttributes.filterValues { it > 0 }.keys
        val readableString = StringBuilder()
        if (nonIncrementalReasons.isEmpty()) {
            readableString.append("Incremental build; ")
        } else {
            nonIncrementalReasons.joinTo(readableString, prefix = "Non incremental build because: [", postfix = "]; ") { it.readableString }
        }
        data.changes.joinTo(readableString, prefix = "Changes: [", postfix = "]; ") { it.substringAfterLast(File.separator) }

        val timeData =
            data.buildTimesMs.map { (key, value) -> "${key.readableString}: ${value}ms" } //sometimes it is better to have separate variable to be able debug
        val perfData = data.perfData.map { (key, value) -> "${key.readableString}: ${readableFileLength(value)}" }
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
