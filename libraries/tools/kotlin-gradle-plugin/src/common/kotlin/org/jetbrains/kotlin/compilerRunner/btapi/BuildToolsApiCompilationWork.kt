/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import com.android.build.gradle.internal.cxx.os.exe
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.COMPILER_ARGUMENTS_LOG_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.compilerRunner.asFinishLogMessage
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.logCompilerArgumentsMessage
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdService
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.getTaskLogger
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.destinationAsFile
import org.jetbrains.kotlin.gradle.utils.stackTraceAsString
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.nio.file.Paths
import javax.inject.Inject

private const val LOGGER_PREFIX = "[KOTLIN] "

internal abstract class BuildToolsApiCompilationWork @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
) :
    WorkAction<BuildToolsApiCompilationWork.BuildToolsApiCompilationParameters> {
    internal interface BuildToolsApiCompilationParameters : WorkParameters {
        val buildIdService: Property<BuildIdService>
        val buildFinishedListenerService: Property<BuildFinishedListenerService>
        val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>
    }

    private val workArguments
        get() = parameters.compilerWorkArguments.get()

    private val taskPath
        get() = workArguments.taskPath

    private val metrics = if (workArguments.reportingSettings.buildReportOutputs.isNotEmpty()) {
        BuildMetricsReporterImpl()
    } else {
        DoNothingBuildMetricsReporter
    }

    private val log: KotlinLogger = getTaskLogger(taskPath, LOGGER_PREFIX, BuildToolsApiCompilationWork::class.java.simpleName, true)

    private fun performCompilation(executionStrategy: KotlinCompilerExecutionStrategy): CompilationResult {
        try {
            val classLoader = parameters.classLoadersCachingService.get()
                .getClassLoader(workArguments.compilerFullClasspath, SharedApiClassesClassLoaderProvider)
            val compilationService = KotlinToolchain.loadImplementation(classLoader)
            val buildId = ProjectId.ProjectUUID(parameters.buildIdService.get().buildId)
            val build = compilationService.createBuildSession()
            parameters.buildFinishedListenerService.get().onCloseOnceByKey(buildId.toString()) {
                build.close()
            }

            val args = parseCommandLineArguments<K2JVMCompilerArguments>(workArguments.compilerArgs.toList())
            val jvmCompilationOperation = compilationService.jvm.createJvmCompilationOperation(
                args.freeArgs.mapNotNull {
                    try {
                        Paths.get(it)
                    } catch (e: Exception) {
                        null
                    }
                },
                args.destinationAsFile.toPath()
            )
            jvmCompilationOperation.compilerArguments.applyArgumentStrings(workArguments.compilerArgs.toList())
            jvmCompilationOperation[KOTLINSCRIPT_EXTENSIONS] = workArguments.kotlinScriptExtensions
            jvmCompilationOperation[COMPILER_ARGUMENTS_LOG_LEVEL] = workArguments.compilerArgumentsLogLevel.value
            if (metrics is BuildMetricsReporterImpl) {
                @Suppress("DEPRECATION_ERROR")
                jvmCompilationOperation[BuildOperation.createCustomOption("XX_KGP_METRICS_COLLECTOR")] = true
            }

            val icEnv = workArguments.incrementalCompilationEnvironment
            val classpathChanges = icEnv?.classpathChanges
            if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
                val classpathSnapshotsOptions = jvmCompilationOperation.createSnapshotBasedIcOptions().apply {
                    this[ROOT_PROJECT_DIR] = icEnv.rootProjectDir.toPath()
                    this[MODULE_BUILD_DIR] = icEnv.buildDir.toPath()
                    this[PRECISE_JAVA_TRACKING] = icEnv.icFeatures.usePreciseJavaTracking
                    this[BACKUP_CLASSES] = icEnv.icFeatures.preciseCompilationResultsBackup
                    this[KEEP_IC_CACHES_IN_MEMORY] = icEnv.icFeatures.keepIncrementalCompilationCachesInMemory
                    this[OUTPUT_DIRS] = workArguments.outputFiles.map { it.toPath() }.toSet()
                    this[FORCE_RECOMPILATION] = classpathChanges !is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun
                    this[USE_FIR_RUNNER] = icEnv.useJvmFirRunner
                    this[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = icEnv.icFeatures.enableUnsafeIncrementalCompilationForMultiplatform
                    this[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = icEnv.icFeatures.enableMonotonousIncrementalCompileSetExpansion
                }
                jvmCompilationOperation[INCREMENTAL_COMPILATION] = JvmSnapshotBasedIncrementalCompilationConfiguration(
                    icEnv.workingDir.toPath(),
                    icEnv.changedFiles,
                    classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.map { it.toPath() },
                    classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.toPath(),
                    classpathSnapshotsOptions
                )

                when (classpathChanges) {
                    is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges -> {
                        classpathSnapshotsOptions[JvmSnapshotBasedIncrementalCompilationOptions.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] =
                            true
                    }
                    is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun -> {
                        classpathSnapshotsOptions[FORCE_RECOMPILATION] = true
                    }
                    else -> {}
                }
            }
            val executionConfig = when (executionStrategy) {
                KotlinCompilerExecutionStrategy.DAEMON -> compilationService.createDaemonExecutionPolicy().apply {
                    val arguments = workArguments.compilerExecutionSettings.daemonJvmArgs ?: emptyList()
                    this[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = arguments
                    if (log.isDebugEnabled) {
                        log.debug("Kotlin compile daemon JVM options: ${arguments.joinToString(" ")}")
                    }
                }
                KotlinCompilerExecutionStrategy.IN_PROCESS -> compilationService.createInProcessExecutionPolicy()
                else -> error("The \"$executionStrategy\" execution strategy is not supported by the Build Tools API")
            }
            return build.executeOperation(jvmCompilationOperation, executionConfig, log)
                .also { extractMetrics(jvmCompilationOperation) }
        } catch (e: Throwable) {
            wrapAndRethrowCompilationException(executionStrategy, e)
        }
    }

    private fun extractMetrics(jvmCompilationOperation: JvmCompilationOperation) {
        if (metrics is BuildMetricsReporterImpl) {
            @Suppress("DEPRECATION_ERROR")
            val key = BuildOperation.createCustomOption<ByteArray>("XX_KGP_METRICS_COLLECTOR_OUT")
            try {
                ByteArrayInputStream(jvmCompilationOperation[key]).use {
                    @Suppress("UNCHECKED_CAST")
                    val metricsFromBta =
                        ObjectInputStream(it).readObject() as BuildMetricsReporterImpl<GradleBuildTime, GradleBuildPerformanceMetric>
                    metrics.addMetrics(metricsFromBta.getMetrics())
                }
            } catch (_: Exception) {
            }
        }
    }

    // the files are backed up in the task action before any changes to the outputs
    private fun initializeBackup(): TaskOutputsBackup? = if (parameters.snapshotsDir.isPresent) {
        TaskOutputsBackup(
            fileSystemOperations,
            parameters.snapshotsDir,
            parameters.taskOutputsToRestore.get(),
            log,
        )
    } else {
        null
    }

    private fun compileWithDaemonOrFallback(
        executionStrategy: KotlinCompilerExecutionStrategy,
        backup: TaskOutputsBackup?,
    ): CompilationResult {
        with(log) {
            kotlinDebug { "Kotlin compiler class: ${workArguments.compilerClassName}" }
            kotlinDebug {
                val compilerClasspath = workArguments.compilerFullClasspath.joinToString(File.pathSeparator) { it.normalize().absolutePath }
                "Kotlin compiler classpath: $compilerClasspath"
            }
            logCompilerArgumentsMessage(workArguments.compilerArgumentsLogLevel) {
                "${workArguments.taskPath} Kotlin compiler args: ${workArguments.compilerArgs.joinToString(" ")}"
            }
        }
        try {
            val result = performCompilation(executionStrategy)
            if (result == CompilationResult.COMPILATION_OOM_ERROR || result == CompilationResult.COMPILATION_ERROR) {
                backup?.restoreOutputs()
            }
            throwExceptionIfCompilationFailed(result.asExitCode, executionStrategy)
            log.info(executionStrategy.asFinishLogMessage)
            return result
        } catch (e: FailedCompilationException) {
            backup?.tryRestoringOnRecoverableException(e) { restoreAction ->
                log.info(DEFAULT_BACKUP_RESTORE_MESSAGE)
                restoreAction()
            }
            throw e
        }
    }

    override fun execute() {
        metrics.addTimeMetric(GradleBuildPerformanceMetric.START_WORKER_EXECUTION)
        metrics.startMeasure(GradleBuildTime.RUN_COMPILATION_IN_WORKER)
        val backup = initializeBackup()
        try {
            compileWithDaemonOrFallback(workArguments.compilerExecutionSettings.strategy, backup)
        } catch (e: FailedCompilationException) {
            if (workArguments.compilerExecutionSettings.strategy == KotlinCompilerExecutionStrategy.DAEMON) {
                val gradlePrintingMessageCollector = GradlePrintingMessageCollector(log, workArguments.allWarningsAsErrors)
                val gradleMessageCollector = GradleErrorMessageCollector(
                    log,
                    gradlePrintingMessageCollector,
                    kotlinPluginVersion = workArguments.kotlinPluginVersion
                )
                gradleMessageCollector.report(
                    severity = CompilerMessageSeverity.EXCEPTION,
                    message = "Daemon compilation failed: ${e.message}\n${e.stackTraceToString()}"
                )
                val recommendation = """
                        Try ./gradlew --stop if this issue persists
                        If it does not look related to your configuration, please file an issue with logs to https://kotl.in/issue
                    """.trimIndent()
                if (!workArguments.compilerExecutionSettings.useDaemonFallbackStrategy) {
                    throw RuntimeException(
                        """
                            |Failed to compile with Kotlin daemon.
                            |Fallback strategy (compiling without Kotlin daemon) is turned off.
                            |$recommendation
                            """.trimMargin(),
                        e
                    )
                }
                val failDetails = e.stackTraceAsString().removeSuffixIfPresent("\n")
                log.warn(
                    """
                        |Failed to compile with Kotlin daemon: $failDetails
                        |Using fallback strategy: Compile without Kotlin daemon
                        |$recommendation
                        """.trimMargin()
                )
                compileWithDaemonOrFallback(
                    KotlinCompilerExecutionStrategy.IN_PROCESS,
                    backup
                )
            } else {
                throw e
            }
        } finally {
            val taskInfo = TaskExecutionInfo(
                kotlinLanguageVersion = workArguments.kotlinLanguageVersion,
                changedFiles = workArguments.incrementalCompilationEnvironment?.changedFiles,
                compilerArguments = if (workArguments.reportingSettings.includeCompilerArguments) workArguments.compilerArgs else emptyArray(),
//                tags = collectStatTags(),
            )
            metrics.endMeasure(GradleBuildTime.RUN_COMPILATION_IN_WORKER)
            val result =
                TaskExecutionResult(buildMetrics = metrics.getMetrics(), taskInfo = taskInfo)
            TaskExecutionResults[workArguments.taskPath] = result
            backup?.deleteSnapshot()
        }
    }


    // temporary adapter property
    private val CompilationResult.asExitCode
        get() = when (this) {
            CompilationResult.COMPILATION_ERROR -> ExitCode.COMPILATION_ERROR
            CompilationResult.COMPILER_INTERNAL_ERROR -> ExitCode.INTERNAL_ERROR
            CompilationResult.COMPILATION_OOM_ERROR -> ExitCode.OOM_ERROR
            else -> ExitCode.OK
        }
}

internal object SharedApiClassesClassLoaderProvider : ParentClassLoaderProvider {
    override fun getClassLoader() = SharedApiClassesClassLoader()

    override fun hashCode() = SharedApiClassesClassLoaderProvider::class.hashCode()

    override fun equals(other: Any?) = other is SharedApiClassesClassLoaderProvider
}
