/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report.data

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics

class BuildExecutionData(
    val startParameters: Collection<String>,
    val failureMessages: List<String?>,
    val taskExecutionData: Collection<TaskExecutionData>
) {
    val aggregatedMetrics by lazy {
        BuildMetrics().also { acc ->
            taskExecutionData.forEach { acc.addAll(it.buildMetrics) }
        }
    }
}