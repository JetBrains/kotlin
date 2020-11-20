/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import java.io.File
import java.io.Serializable

data class ReportingSettings(
    val metricsOutputFile: File? = null,
    val buildReportDir: File? = null,
    val reportMetrics: Boolean = false,
    val includeMetricsInReport: Boolean = false,
    val buildReportMode: BuildReportMode = BuildReportMode.NONE
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}