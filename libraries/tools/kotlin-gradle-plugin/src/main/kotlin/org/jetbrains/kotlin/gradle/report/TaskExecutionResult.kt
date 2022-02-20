/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.incremental.ChangedFiles

internal class TaskExecutionResult(
    val buildMetrics: BuildMetrics,
    val taskInfo: TaskExecutionInfo = TaskExecutionInfo(),
    val icLogLines: List<String> = emptyList()
)

internal class TaskExecutionInfo(
    val changedFiles: ChangedFiles? = null,
    val compilerArguments: Array<String> = emptyArray()
)
