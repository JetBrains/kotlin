package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.utils.pathsAsStringRelativeTo
import java.io.File
import java.io.Serializable
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

internal class GradleCompilationResults(
    private val log: KotlinLogger,
    private val projectRootFile: File
) : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {

    var icLogLines: List<String> = emptyList()
    private val buildMetricsReporter = BuildMetricsReporterImpl<GradleBuildTime, GradleBuildPerformanceMetric>()
    val buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>
        get() = buildMetricsReporter.getMetrics()

    @Throws(RemoteException::class)
    override fun add(compilationResultCategory: Int, value: Serializable) {
        when (compilationResultCategory) {
            CompilationResultCategory.IC_COMPILE_ITERATION.code -> {
                @Suppress("UNCHECKED_CAST")
                val compileIterationResult = value as? CompileIterationResult
                if (compileIterationResult != null) {
                    val sourceFiles = compileIterationResult.sourceFiles
                    if (sourceFiles.any()) {
                        log.kotlinDebug { "compile iteration: ${sourceFiles.pathsAsStringRelativeTo(projectRootFile)}" }
                        buildMetrics.buildPerformanceMetrics.add(GradleBuildPerformanceMetric.COMPILE_ITERATION)
                    }
                    val exitCode = compileIterationResult.exitCode
                    log.kotlinDebug { "compiler exit code: $exitCode" }
                }
            }
            CompilationResultCategory.BUILD_REPORT_LINES.code,
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code -> {
                @Suppress("UNCHECKED_CAST")
                (value as? List<String>)?.let { icLogLines = it }
            }
            CompilationResultCategory.BUILD_METRICS.code -> {
                (value as? BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>)?.let { buildMetricsReporter.addMetrics(it) }
            }
        }
    }
}