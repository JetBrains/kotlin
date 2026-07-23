/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.compilerRunner.asFinishLogMessage
import org.jetbrains.kotlin.compilerRunner.btapi.js.JsKlibBuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.js.JsKlibIncrementalConfigurationStrategy
import org.jetbrains.kotlin.compilerRunner.btapi.js.JsLinkingBuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.jvm.JvmBuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.jvm.JvmIncrementalConfigurationStrategy
import org.jetbrains.kotlin.compilerRunner.btapi.metadata.MetadataKlibBuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.wasm.WasmKlibBuildOperationFactory
import org.jetbrains.kotlin.compilerRunner.btapi.wasm.WasmKlibIncrementalConfigurationStrategy
import org.jetbrains.kotlin.compilerRunner.btapi.wasm.WasmLinkingBuildOperationFactory
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.logging.*
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdService
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.getTaskLogger
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.report.collectIcTags
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.File
import javax.inject.Inject

internal abstract class BuildToolsApiCompilationWork @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val objects: ObjectFactory,
) :
    WorkAction<BuildToolsApiCompilationWork.BuildToolsApiCompilationParameters> {
    internal interface BuildToolsApiCompilationParameters : WorkParameters, UsesKotlinToolingDiagnosticsParameters {
        val buildSessionService: Property<BuildSessionService>
        val buildIdService: Property<BuildIdService>
        val buildFinishedListenerService: Property<BuildFinishedListenerService>
        val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>>
        val compilerDiagnosticsProblemsReporterFactory: Property<CompilerDiagnosticsProblemsReporter.Factory>
        val warningModeIsAll: Property<Boolean>
    }

    private val workArguments
        get() = parameters.compilerWorkArguments.get()

    private val taskPath
        get() = workArguments.taskPath

    private val compilerDiagnosticsProblemsReporter: CompilerDiagnosticsProblemsReporter
        get() = parameters.compilerDiagnosticsProblemsReporterFactory.get().getInstance(objects)

    private val metrics = if (workArguments.reportingSettings.buildReportOutputs.isNotEmpty()) {
        BuildMetricsReporterImpl()
    } else {
        DoNothingBuildMetricsReporter
    }

    // the files are backed up in the task action before any changes to the outputs
    private fun initializeBackup(log: KotlinLogger): TaskOutputsBackup? = if (parameters.snapshotsDir.isPresent) {
        TaskOutputsBackup(
            fileSystemOperations,
            parameters.snapshotsDir,
            parameters.taskOutputsToRestore.get(),
            log,
        )
    } else {
        null
    }

    /**
     * Fallback to in-process on any unexpected problem, like daemon communication exception or uncaught compiler exception.
     * Normally, valid [CompilationResult] does not lead to a fallback except [CompilationResult.COMPILER_INTERNAL_ERROR].
     */
    private fun compileInDaemon(
        buildSession: KotlinToolchains.BuildSession,
        runner: BtaCompilerRunner<*>,
        log: KotlinLogger,
        tryFallback: Boolean,
        compilerMessageRenderer: ProblemsApiCompilerMessageRenderer,
        // Tasks created via KotlinJvmFactory does not have collector available
        toolingDiagnosticsCollector: KotlinToolingDiagnosticsCollector?,
    ): Pair<CompilationResult, KotlinCompilerExecutionStrategy> {
        val fallback: (Throwable?, ExecutionPolicy?) -> Pair<CompilationResult, KotlinCompilerExecutionStrategy> = { t, executionPolicy ->
            val daemonLogPath = if (executionPolicy is ExecutionPolicy.WithDaemon) {
                executionPolicy[ExecutionPolicy.WithDaemon.LOGS_PATH]
            } else {
                null
            }

            toolingDiagnosticsCollector?.report(
                parameters,
                KotlinToolingDiagnostics.KotlinCompilationInDaemonHasFailed(
                    t,
                    tryFallback,
                    daemonLogPath,
                )
            )

            if (tryFallback) {
                // We don't want to fail the build on Gradle 9.6+ because of error diagnostic from initial attempt
                compilerMessageRenderer.lowerErrorSeveritiesToWarning()

                val compilationResult = runner.performCompilation(
                    buildSession,
                    KotlinCompilerExecutionStrategy.IN_PROCESS,
                    log,
                    compilerMessageRenderer,
                )
                compilationResult.first to KotlinCompilerExecutionStrategy.IN_PROCESS
            } else {
                CompilationResult.COMPILER_INTERNAL_ERROR to KotlinCompilerExecutionStrategy.DAEMON
            }
        }

        return try {
            val daemonCompilationResult = runner.performCompilation(
                buildSession,
                KotlinCompilerExecutionStrategy.DAEMON,
                log,
                compilerMessageRenderer,
            )
            if (daemonCompilationResult.first != CompilationResult.COMPILER_INTERNAL_ERROR) {
                daemonCompilationResult.first to KotlinCompilerExecutionStrategy.DAEMON
            } else {
                fallback(null, daemonCompilationResult.second)
            }
        } catch (t: Throwable) {
            fallback(t, null)
        }
    }

    override fun execute() {
        metrics.addTimeMetric(START_WORKER_EXECUTION)
        metrics.startMeasure(RUN_COMPILATION_IN_WORKER)
        val exceptionReportingKotlinLogger = ExceptionReportingKotlinLogger()
        val printingLogger =
            CapturingDelegatingKotlinLogger(
                getTaskLogger(taskPath, null, BuildToolsApiCompilationWork::class.java.simpleName, true),
                workArguments.reportingSettings.buildReportMode,
            )
        val log: KotlinLogger = CompositeKotlinLogger(
            setOf(
                printingLogger,
                exceptionReportingKotlinLogger,
            )
        )
        val compilerMessageRenderer = ProblemsApiCompilerMessageRenderer(
            suppressLogForProblemsApi = parameters.warningModeIsAll.getOrElse(false),
        )
        val runner = createRunner(workArguments.btaToolchain ?: error("btaToolchain is not set for task ${workArguments.taskPath}"), workArguments, metrics)
        val backup = initializeBackup(log)
        val buildSession = obtainBuildSession()

        try {
            with(log) {
                kotlinDebug { "Kotlin compiler class: ${workArguments.compilerClassName}" }
                kotlinDebug {
                    val compilerClasspath =
                        workArguments.compilerFullClasspath.joinToString(File.pathSeparator) { it.normalize().absolutePath }
                    "Kotlin compiler classpath: $compilerClasspath"
                }
            }

            val executionStrategy = workArguments.compilerExecutionSettings.strategy
            val (compilationResult, effectiveExecutionStrategy) = when (executionStrategy) {
                KotlinCompilerExecutionStrategy.DAEMON -> {
                    val tryFallback = workArguments.compilerExecutionSettings.useDaemonFallbackStrategy
                    compileInDaemon(
                        buildSession,
                        runner,
                        log,
                        tryFallback,
                        compilerMessageRenderer,
                        parameters.toolingDiagnosticsCollector.orNull,
                    )
                }
                KotlinCompilerExecutionStrategy.IN_PROCESS -> runner.performCompilation(
                    buildSession,
                    KotlinCompilerExecutionStrategy.IN_PROCESS,
                    log,
                    compilerMessageRenderer,
                ).first to KotlinCompilerExecutionStrategy.IN_PROCESS
            }
            log.info(effectiveExecutionStrategy.asFinishLogMessage)

            throwExceptionIfCompilationFailed(compilationResult.asExitCode, executionStrategy)
        } catch (e: FailedCompilationException) {
            // Restore outputs only for CompilationErrorException or OOMErrorException (see GradleKotlinCompilerWorkAction.execute)
            backup?.tryRestoringOnRecoverableException(e) { restoreAction ->
                metrics.measure(RESTORE_OUTPUT_FROM_BACKUP) {
                    log.info(DEFAULT_BACKUP_RESTORE_MESSAGE)
                    restoreAction()
                }
            }
            throw e
        } finally {
            val taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = workArguments.kotlinLanguageVersion,
                changedFiles = workArguments.incrementalCompilationEnvironment?.changedFiles,
                compilerArguments = if (workArguments.reportingSettings.includeCompilerArguments) workArguments.compilerArgs else emptyArray(),
                tags = workArguments.incrementalCompilationEnvironment?.collectIcTags().orEmpty(),
            )
            workArguments.errorsFiles?.let {
                /*
                 * Use `printingLogger` here intentionally (not `log`) to avoid accidentally writing
                 * to the exception-reporting logger.
                 * After we extract messages from
                 * `exceptionReportingKotlinLogger`, it should remain silent.
                 * The extractor asserts this
                 * and will flag any further writes as misuse.
                 */
                exceptionReportingKotlinLogger.extractExceptionMessages()
                    .reportToIde(it, workArguments.kotlinPluginVersion, logger = printingLogger)
            }
            metrics.endMeasure(RUN_COMPILATION_IN_WORKER)
            val result =
                TaskExecutionResult(buildMetrics = metrics.getMetrics(), taskInfo = taskInfo, icLogLines = printingLogger.capturedLines)
            TaskExecutionResults[workArguments.taskPath] = result
            backup?.deleteSnapshot()

            // Replay buffered compiler diagnostics on the worker thread (see ProblemsApiCompilerMessageRenderer).
            // Throws an exception on error, so should be last in `finally` block
            compilerMessageRenderer.replayTo(compilerDiagnosticsProblemsReporter)
        }
    }

    private fun obtainBuildSession(): KotlinToolchains.BuildSession = parameters.buildSessionService.get().getOrCreateBuildSession(
        parameters.classLoadersCachingService.get(),
        workArguments.compilerFullClasspath
    )

}

private fun createRunner(
    toolchain: BtaToolchain,
    workArguments: GradleKotlinCompilerWorkArguments,
    metrics: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>,
): BtaCompilerRunner<*> {
    val icEnv = workArguments.incrementalCompilationEnvironment
    val outputDirs = workArguments.outputFiles.map(File::toPath)
    val compilerArgs = workArguments.compilerArgs.toList()
    val daemonJvmArgs = workArguments.compilerExecutionSettings.daemonJvmArgs ?: emptyList()
    val compilerArgumentsLogLevel = workArguments.compilerArgumentsLogLevel.toBtaCompilerArgumentsLogLevel()
    val generateCompilerRefIndex = workArguments.compilerExecutionSettings.generateCompilerRefIndex

    return when (toolchain) {
        BtaToolchain.JVM -> BtaCompilerRunner(
            metrics,
            JvmBuildOperationFactory(compilerArgs, workArguments.kotlinScriptExtensions.toList()),
            icEnv?.let {
                JvmIncrementalConfigurationStrategy(icEnv, outputDirs)
            } ?: IncrementalConfigurationStrategy.Default,
            daemonJvmArgs,
            compilerArgumentsLogLevel,
            generateCompilerRefIndex,
        )
        BtaToolchain.JS_COMPILATION -> BtaCompilerRunner(
            metrics,
            JsKlibBuildOperationFactory(compilerArgs),
            icEnv?.let {
                JsKlibIncrementalConfigurationStrategy(icEnv, workArguments.incrementalModuleInfo, outputDirs)
            } ?: IncrementalConfigurationStrategy.Default,
            daemonJvmArgs,
            compilerArgumentsLogLevel,
            generateCompilerRefIndex,
        )
        BtaToolchain.JS_LINKING -> BtaCompilerRunner(
            metrics,
            JsLinkingBuildOperationFactory(compilerArgs),
            IncrementalConfigurationStrategy.Default,
            daemonJvmArgs,
            compilerArgumentsLogLevel,
            generateCompilerRefIndex,
        )
        BtaToolchain.WASM_COMPILATION -> BtaCompilerRunner(
            metrics,
            WasmKlibBuildOperationFactory(compilerArgs),
            icEnv?.let {
                WasmKlibIncrementalConfigurationStrategy(icEnv, workArguments.incrementalModuleInfo, outputDirs)
            } ?: IncrementalConfigurationStrategy.Default,
            daemonJvmArgs,
            compilerArgumentsLogLevel,
            generateCompilerRefIndex,
        )
        BtaToolchain.WASM_LINKING -> BtaCompilerRunner(
            metrics,
            WasmLinkingBuildOperationFactory(compilerArgs),
            IncrementalConfigurationStrategy.Default,
            daemonJvmArgs,
            compilerArgumentsLogLevel,
            generateCompilerRefIndex,
        )
        BtaToolchain.METADATA -> BtaCompilerRunner(
            metrics,
            MetadataKlibBuildOperationFactory(compilerArgs),
            IncrementalConfigurationStrategy.Default,
            daemonJvmArgs,
            compilerArgumentsLogLevel,
            generateCompilerRefIndex,
        )
    }
}

private val CompilationResult.asExitCode
    get() = when (this) {
        CompilationResult.COMPILATION_ERROR -> ExitCode.COMPILATION_ERROR
        CompilationResult.COMPILER_INTERNAL_ERROR -> ExitCode.INTERNAL_ERROR
        CompilationResult.COMPILATION_OOM_ERROR -> ExitCode.OOM_ERROR
        else -> ExitCode.OK
    }

private fun KotlinCompilerArgumentsLogLevel.toBtaCompilerArgumentsLogLevel() =
    when (this) {
        KotlinCompilerArgumentsLogLevel.ERROR -> JvmCompilationOperation.CompilerArgumentsLogLevel.ERROR
        KotlinCompilerArgumentsLogLevel.WARNING -> JvmCompilationOperation.CompilerArgumentsLogLevel.WARNING
        KotlinCompilerArgumentsLogLevel.INFO -> JvmCompilationOperation.CompilerArgumentsLogLevel.INFO
        KotlinCompilerArgumentsLogLevel.DEBUG -> JvmCompilationOperation.CompilerArgumentsLogLevel.DEBUG
    }

internal object SharedApiClassesClassLoaderProvider : ParentClassLoaderProvider {
    override fun getClassLoader() = SharedApiClassesClassLoader()

    override fun hashCode() = SharedApiClassesClassLoaderProvider::class.hashCode()

    override fun equals(other: Any?) = other is SharedApiClassesClassLoaderProvider
}
