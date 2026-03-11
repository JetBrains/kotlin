/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.wrapAndRethrowCompilationException
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

internal class BtaCompilerRunner<T : BaseCompilationOperation.Builder>(
    private val metrics: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>,
    private val buildOperationFactory: BuildOperationFactory<T>,
    private val icConfigurator: IncrementalConfigurationStrategy<T>,
    private val daemonJvmArgs: List<String>,
    private val compilerArgumentsLogLevel: JvmCompilationOperation.CompilerArgumentsLogLevel,
    private val generateCompilerRefIndex: Boolean,
) {

    fun performCompilation(
        buildSession: KotlinToolchains.BuildSession,
        executionStrategy: KotlinCompilerExecutionStrategy,
        log: KotlinLogger,
        compilerMessageRenderer: ProblemsApiCompilerMessageRenderer,
    ): CompilationResult {
        try {
            val kotlinToolchains = buildSession.kotlinToolchains
            val compilationOperationBuilder = buildOperationFactory.createOperation(kotlinToolchains)
            setupBaseCompilationSettings(compilationOperationBuilder, compilerMessageRenderer)
            icConfigurator.configureIncrementalCompilationConfiguration(compilationOperationBuilder)
            val compilationOperation = compilationOperationBuilder.build()

            val executionConfig = when (executionStrategy) {
                KotlinCompilerExecutionStrategy.DAEMON -> kotlinToolchains.daemonExecutionPolicy {
                    ExecutionPolicy.WithDaemon.JVM_ARGUMENTS(daemonJvmArgs)
                    if (log.isDebugEnabled) {
                        log.debug("Kotlin compile daemon JVM options: ${daemonJvmArgs.joinToString(" ")}")
                    }
                }
                KotlinCompilerExecutionStrategy.IN_PROCESS -> kotlinToolchains.createInProcessExecutionPolicy()
            }
            return metrics.measure(RUN_COMPILATION) {
                buildSession.executeOperation(compilationOperation, executionConfig, log)
            }.also { extractMetrics(metrics, compilationOperation) }
        } catch (e: Throwable) {
            wrapAndRethrowCompilationException(executionStrategy, e)
        }
    }

    private fun setupBaseCompilationSettings(
        compilationOperationBuilder: BaseCompilationOperation.Builder,
        compilerMessageRenderer: ProblemsApiCompilerMessageRenderer,
    ) {
        compilationOperationBuilder[BaseCompilationOperation.COMPILER_ARGUMENTS_LOG_LEVEL] = compilerArgumentsLogLevel
        compilationOperationBuilder[BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] = compilerMessageRenderer
        if (metrics is BuildMetricsReporterImpl) {
            @Suppress("DEPRECATION_ERROR")
            compilationOperationBuilder[BuildOperation.Companion.createCustomOption("XX_KGP_METRICS_COLLECTOR")] = true
        }
        compilationOperationBuilder[BaseCompilationOperation.GENERATE_COMPILER_REF_INDEX] = generateCompilerRefIndex
    }
}

private fun extractMetrics(
    metrics: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>,
    compilationOperation: BaseCompilationOperation,
) {
    if (metrics is BuildMetricsReporterImpl) {
        @Suppress("DEPRECATION_ERROR")
        val key = BuildOperation.createCustomOption<ByteArray>("XX_KGP_METRICS_COLLECTOR_OUT")
        try {
            ByteArrayInputStream(compilationOperation[key]).use {
                @Suppress("UNCHECKED_CAST")
                val metricsFromBta =
                    ObjectInputStream(it).readObject() as BuildMetricsReporterImpl<GradleBuildTimeMetric, GradleBuildPerformanceMetric>
                metrics.addMetrics(metricsFromBta.getMetrics())
            }
        } catch (_: Exception) {
        }
    }
}
