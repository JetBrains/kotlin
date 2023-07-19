/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report.data

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.statistics.BuildStartParameters

class BuildExecutionData(
    val startParameters: BuildStartParameters,
    val failureMessages: List<String?>,
    val buildOperationRecord: Collection<BuildOperationRecord>
) {
    val aggregatedMetrics by lazy {
        BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>().also { acc ->
            buildOperationRecord.forEach { acc.addAll(it.buildMetrics) }
        }
    }
}