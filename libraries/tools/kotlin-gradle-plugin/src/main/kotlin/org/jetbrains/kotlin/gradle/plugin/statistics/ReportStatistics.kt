/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.stat

data class CompileStatData(
    val version: Int = 1,
    val projectName: String?,
    val label: String?,
    val taskName: String?,
    val taskResult: String,
    val duration: Long,
    val tags: List<String>,
    val changes: List<String>,
    val statData: Map<String, Long>
)

interface ReportStatistics {
    fun report(data: CompileStatData)
}

