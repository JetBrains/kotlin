/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import com.google.gson.Gson
import com.gradle.scan.plugin.BuildScanExtension
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics

class ReportStatisticsToBuildScan(val buildScan: BuildScanExtension) : ReportStatistics {
    override fun report(data: CompileStatData) {
        buildScan.value(data.taskName, Gson().toJson(data).toString())
    }
}