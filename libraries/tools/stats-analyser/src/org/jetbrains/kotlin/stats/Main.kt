/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import java.io.File

fun main(args: Array<String>) {
    val fileOrDirectoryPath = args.firstOrNull() ?: run {
        println("Usage: <path-to-directory-or-single-json-file> [module-name]")
        println("If [module-name] is specified, the tool prints stats for all found time stamps of the single specified module.")
        return
    }

    val analysedModule = args.getOrNull(1)

    val jsonReportFiles: Array<File> = FileUtils.extractJsonReportFiles(fileOrDirectoryPath) ?: return

    val reportsData: ReportsData = FileUtils.deserializeJsonReports(jsonReportFiles, analysedModule)

    val statsCalculator = StatsCalculator(reportsData)

    print(MarkdownReportRenderer(statsCalculator).render())
}
