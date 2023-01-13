/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

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
    val includeCompilerArguments: Boolean = false,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 1
    }
}

data class FileReportSettings(
    val buildReportDir: File,
    val includeMetricsInReport: Boolean = false,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}

data class HttpReportSettings(
    val url: String,
    val password: String?,
    val user: String?,
    val verboseEnvironment: Boolean,
    val includeGitBranchName: Boolean
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}

data class BuildScanSettings(
    val customValueLimit: Int,
): Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}