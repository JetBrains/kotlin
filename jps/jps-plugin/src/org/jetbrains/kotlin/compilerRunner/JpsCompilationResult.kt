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
        val buildTimes =  buildMetrics.buildTimes
        val buildPerformanceMetrics = buildMetrics.buildPerformanceMetrics
        when (compilationResultCategory) {
            CompilationResultCategory.IC_COMPILE_ITERATION.code -> {
                @Suppress("UNCHECKED_CAST")
                val compileIterationResult = value as? CompileIterationResult
                if (compileIterationResult != null) {
                    val sourceFiles = compileIterationResult.sourceFiles
                    buildPerformanceMetrics.add(JpsBuildPerformanceMetric.IC_COMPILE_ITERATION)
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
                        CompilationPerformanceMetrics.COMPILER_INITIALIZATION -> buildTimes.addTimeMs(
                            JpsBuildTime.COMPILER_INITIALIZATION,
                            it.value
                        )
                        CompilationPerformanceMetrics.CODE_ANALYSIS -> buildTimes.addTimeMs(JpsBuildTime.CODE_ANALYSIS, it.value)
                        CompilationPerformanceMetrics.IR_TRANSLATION -> buildTimes.addTimeMs(JpsBuildTime.IR_TRANSLATION, it.value)
                        CompilationPerformanceMetrics.IR_LOWERING -> buildTimes.addTimeMs(JpsBuildTime.IR_LOWERING, it.value)
                        CompilationPerformanceMetrics.BACKEND_OR_METADATA_GENERATION -> buildTimes.addTimeMs(
                            JpsBuildTime.BACKEND_OR_METADATA_GENERATION,
                            it.value
                        )

                        CompilationPerformanceMetrics.SOURCE_LINES_NUMBER -> buildPerformanceMetrics.add(
                            JpsBuildPerformanceMetric.SOURCE_LINES_NUMBER,
                            it.value
                        )
                        CompilationPerformanceMetrics.ANALYSIS_LPS -> buildPerformanceMetrics.add(
                            JpsBuildPerformanceMetric.ANALYSIS_LPS,
                            it.value
                        )
                        CompilationPerformanceMetrics.IR_TRANSLATION_LPS -> buildPerformanceMetrics.add(
                            JpsBuildPerformanceMetric.IR_TRANSLATION_LPS,
                            it.value
                        )
                        CompilationPerformanceMetrics.IR_LOWERING_LPS -> buildPerformanceMetrics.add(
                            JpsBuildPerformanceMetric.IR_LOWERING_LPS,
                            it.value
                        )
                        CompilationPerformanceMetrics.BACKEND_OR_METADATA_GENERATION_LPS -> buildPerformanceMetrics.add(
                            JpsBuildPerformanceMetric.BACKEND_OR_METADATA_GENERATION_LPS,
                            it.value
                        )
                    }
                }
            }
        }
    }
}