/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.jetbrains.kotlin.build.report.FileReportSettings
import org.jetbrains.kotlin.build.report.HttpReportSettings
import java.io.File
import java.io.Serializable

data class ReportingSettings(
    val buildReportOutputs: List<BuildReportType> = emptyList(),
    val buildReportMode: BuildReportMode = BuildReportMode.NONE,
    val buildReportLabel: String? = null,
    val fileReportSettings: FileReportSettings? = null,
    val httpReportSettings: HttpReportSettings? = null,
    val buildScanReportSettings: BuildScanSettings? = null,
    val singleOutputFile: File? = null,
    val experimentalTryNextConsoleOutput: Boolean = false,
    val includeCompilerArguments: Boolean = false,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 2L
    }
}

data class BuildScanSettings(
    val customValueLimit: Int,
    val metrics: Set<String>?
): Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}