/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report.data

import org.gradle.api.Task
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics

interface TaskExecutionData {
    val taskPath: String
    val startMs: Long
    val endMs: Long
    val totalTimeMs: Long
    val skipMessage: String?
    val didWork: Boolean
    val icLogLines: List<String>
    val buildMetrics: BuildMetrics
    val isKotlinTask: Boolean
    val type: String
}

