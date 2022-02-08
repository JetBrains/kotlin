/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.stat

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import java.text.SimpleDateFormat

data class CompileStatData(
    val version: Int = 1,
    val projectName: String?,
    val label: String?,
    val taskName: String?,
    val taskResult: String,
    val durationMs: Long,
    val tags: List<String>,
    val changes: List<String>,
    val buildUuid: String = "Unset",
    val kotlinVersion: String = "0.0.0",
    val hostName: String? = "Unset",
    val timeInMillis: Long,
    val timestamp: String = formatter.format(timeInMillis),
    val nonIncrementalAttributes: Map<BuildAttribute, Int>,
    val buildTimesMs: Map<BuildTime, Long>,
    val perfData: Map<BuildPerformanceMetric, Long>
) {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }
}


interface ReportStatistics {
    fun report(data: CompileStatData)
}

