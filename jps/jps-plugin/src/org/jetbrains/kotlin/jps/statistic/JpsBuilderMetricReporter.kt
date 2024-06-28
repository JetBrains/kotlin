/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.JpsBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.JpsBuildTime
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.compilerRunner.JpsCompilationResult

interface JpsBuilderMetricReporter : BuildMetricsReporter<JpsBuildTime, JpsBuildPerformanceMetric> {
    fun flush(context: CompileContext): JpsCompileStatisticsData

    fun buildFinish(moduleChunk: ModuleChunk, context: CompileContext, exitCode: String)

    fun addChangedFiles(files: List<String>)

    fun addCompilerArguments(arguments: List<String>)

    fun setKotlinLanguageVersion(languageVersion: String?)

    fun addTag(tag: StatTag)

    fun addCompilerMetrics(jpsCompilationResult: JpsCompilationResult)
}