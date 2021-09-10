/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.stat

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import java.text.SimpleDateFormat
import java.util.*

//Sensitive data. This object is used directly for statistic via http
data class CompileStatisticsData(
    val version: Int = 2,
    val projectName: String?,
    val label: String?,
    val taskName: String?,
    val taskResult: String,
    val durationMs: Long,
    val tags: List<String>,
    val changes: List<String>,
    val buildUuid: String = "Unset",
    val kotlinVersion: String,
    val hostName: String? = "Unset",
    val finishTime: Long,
    val timestamp: String = formatter.format(finishTime),
    val compilerArguments: List<String>,
    val nonIncrementalAttributes: Set<BuildAttribute>,
    //TODO think about it,time in milliseconds
    val buildTimesMetrics: Map<BuildTime, Long>,
    val performanceMetrics: Map<BuildPerformanceMetric, Long>
) {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").also { it.timeZone = TimeZone.getTimeZone("UTC")}
    }
}

enum class StatTag {
    ABI_SNAPSHOT,
    ARTIFACT_TRANSFORM,
    INCREMENTAL,
    NON_INCREMENTAL,
    GRADLE_DEBUG,
    KOTLIN_DEBUG
}

