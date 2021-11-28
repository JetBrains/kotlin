/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import com.gradle.scan.plugin.BuildScanExtension
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.konan.file.File

class ReportStatisticsToBuildScan(
    private val buildScan: BuildScanExtension
) : ReportStatistics {
    companion object {
        const val kbSize = 1024
        const val mbSize = kbSize * kbSize
        const val gbSize = kbSize * mbSize
    }

    override fun report(data: CompileStatData) {
        buildScan.value(data.taskName, readableString(data))
        data.tags.forEach { buildScan.tag(it) }
    }

    private fun readableString(data: CompileStatData): String {
        val nonIncrementalReasons = data.nonIncrementalAttributes.filterValues { it > 0 }.keys
        val readableString = StringBuilder()
        if (nonIncrementalReasons.isEmpty()) {
            readableString.append("Incremental build; ")
        } else {
            nonIncrementalReasons.joinTo(readableString, prefix = "Non incremental build because: [", postfix = "]; ") { it.readableString }
        }
        data.changes.joinTo(readableString, prefix = "Changes: [", postfix = "]; ") { it.substringAfterLast(File.separator) }

        val timeData = data.timeData.map { (key, value) -> "${key.readableString}: ${value}ms"} //sometimes it is better to have separate variable to be able debug
        val perfData = data.perfData.map { (key, value) -> "$key: ${readableFileLength(value)}"}
        timeData.union(perfData).joinTo(readableString, ",", "Performance: [", "]")
        return readableString.toString()
    }


    private fun readableFileLength(length: Long): String =
        when {
            length / gbSize > 0 -> "${length / gbSize} GB"
            length / mbSize > 0 -> "${length / mbSize} MB"
            length / kbSize > 0 -> "${length / kbSize} KB"
            else -> "$length B"
        }
}
