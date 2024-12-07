/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import java.io.Serializable
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

class JpsCompilationResult : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {

    var icLogLines: List<String> = emptyList()
    val compiledFiles = ArrayList<String>()

    private val buildMetricsReporter = BuildMetricsReporterImpl<JpsBuildTime, JpsBuildPerformanceMetric>()
    val buildMetrics: BuildMetrics<JpsBuildTime, JpsBuildPerformanceMetric>
        get() = buildMetricsReporter.getMetrics()
    private val log = JpsKotlinLogger(KotlinBuilder.LOG)
    @Throws(RemoteException::class)
    override fun add(compilationResultCategory: Int, value: Serializable) {
        when (compilationResultCategory) {
            CompilationResultCategory.IC_COMPILE_ITERATION.code -> {
                @Suppress("UNCHECKED_CAST")
                val compileIterationResult = value as? CompileIterationResult
                if (compileIterationResult != null) {
                    val sourceFiles = compileIterationResult.sourceFiles
                    buildMetrics.buildPerformanceMetrics.add(JpsBuildPerformanceMetric.IC_COMPILE_ITERATION)
                    compiledFiles.addAll(sourceFiles.map { it.path })
                }
            }
            CompilationResultCategory.BUILD_REPORT_LINES.code,
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code -> {
                @Suppress("UNCHECKED_CAST")
                (value as? List<String>)?.let { icLogLines = it }
            }
            CompilationResultCategory.BUILD_METRICS.code -> {
                (value as BuildMetricsValue).let {
                    when (it.key) {
                        CompilationPerformanceMetrics.CODE_GENERATION -> buildMetrics.buildTimes.addTimeMs(JpsBuildTime.CODE_GENERATION, it.value)
                        CompilationPerformanceMetrics.CODE_ANALYSIS -> buildMetrics.buildTimes.addTimeMs(JpsBuildTime.CODE_ANALYSIS, it.value)
                        CompilationPerformanceMetrics.COMPILER_INITIALIZATION -> buildMetrics.buildTimes.addTimeMs(JpsBuildTime.COMPILER_INITIALIZATION, it.value)

                        CompilationPerformanceMetrics.ANALYZED_LINES_NUMBER -> buildMetrics.buildPerformanceMetrics.add(JpsBuildPerformanceMetric.ANALYZED_LINES_NUMBER, it.value)
                        CompilationPerformanceMetrics.ANALYSIS_LPS -> buildMetrics.buildPerformanceMetrics.add(JpsBuildPerformanceMetric.ANALYSIS_LPS, it.value)
                        CompilationPerformanceMetrics.CODE_GENERATED_LINES_NUMBER -> buildMetrics.buildPerformanceMetrics.add(JpsBuildPerformanceMetric.CODE_GENERATED_LINES_NUMBER, it.value)
                        CompilationPerformanceMetrics.CODE_GENERATION_LPS -> buildMetrics.buildPerformanceMetrics.add(JpsBuildPerformanceMetric.CODE_GENERATION_LPS, it.value)
                    }
                }
            }
        }
    }
}