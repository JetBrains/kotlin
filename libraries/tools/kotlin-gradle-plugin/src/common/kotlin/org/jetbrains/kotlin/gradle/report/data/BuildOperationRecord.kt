/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report.data

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime

/** Data for a build operation (e.g., task or transform). */
interface BuildOperationRecord {
    val path: String
    val classFqName: String
    val isFromKotlinPlugin: Boolean
    val startTimeMs: Long // Measured by System.currentTimeMillis()
    val totalTimeMs: Long
    val buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>
    val didWork: Boolean
    val skipMessage: String?
    val icLogLines: List<String>
}
