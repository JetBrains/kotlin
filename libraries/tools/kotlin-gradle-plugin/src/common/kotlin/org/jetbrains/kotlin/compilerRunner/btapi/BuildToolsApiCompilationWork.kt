/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

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
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.COMPILER_ARGUMENTS_LOG_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilationResults
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.compilerRunner.asFinishLogMessage
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdService
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.getTaskLogger
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.destinationAsFile
import org.jetbrains.kotlin.incremental.ClasspathChanges
import java.io.File
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

    private fun performCompilation(): CompilationResult {
        val executionStrategy = workArguments.compilerExecutionSettings.strategy
        try {
            val classLoader = parameters.classLoadersCachingService.get()
                .getClassLoader(workArguments.compilerFullClasspath, SharedApiClassesClassLoaderProvider)
            val compilationService = KotlinToolchain.loadImplementation(classLoader)
            val buildId = ProjectId.ProjectUUID(parameters.buildIdService.get().buildId)
            val build = compilationService.createBuildSession()
            parameters.buildFinishedListenerService.get().onCloseOnceByKey(buildId.toString()) {
                build.close()
            }
            val executionConfig = when (executionStrategy) {
                KotlinCompilerExecutionStrategy.DAEMON -> compilationService.createDaemonExecutionPolicy().apply {
                    this[ExecutionPolicy.WithDaemon.Companion.JVM_ARGUMENTS] =
                        workArguments.compilerExecutionSettings.daemonJvmArgs ?: emptyList()
                }
                KotlinCompilerExecutionStrategy.IN_PROCESS -> compilationService.createInProcessExecutionPolicy()
                else -> error("The \"$executionStrategy\" execution strategy is not supported by the Build Tools API")
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
            @Suppress("DEPRECATION_ERROR")
            jvmCompilationOperation[BuildOperation.Companion.createCustomOption("XX_KGP_METRICS_COLLECTOR")] = metrics

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
                        classpathSnapshotsOptions[JvmSnapshotBasedIncrementalCompilationOptions.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] =
                            true
                    }
                    is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun -> {
                        classpathSnapshotsOptions[FORCE_RECOMPILATION] = true
                    }
                    else -> {}
                }
            }
            return build.executeOperation(jvmCompilationOperation, executionConfig, log)
        } catch (e: Throwable) {
            wrapAndRethrowCompilationException(executionStrategy, e)
        } finally {
            log.info(executionStrategy.asFinishLogMessage)
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

    override fun execute() {
        metrics.addTimeMetric(GradleBuildPerformanceMetric.START_WORKER_EXECUTION)
        metrics.startMeasure(GradleBuildTime.RUN_COMPILATION_IN_WORKER)
        val backup = initializeBackup()
        val executionStrategy = workArguments.compilerExecutionSettings.strategy
        try {
            val result = performCompilation()
            if (result == CompilationResult.COMPILATION_OOM_ERROR || result == CompilationResult.COMPILATION_ERROR) {
                backup?.restoreOutputs()
            }
            throwExceptionIfCompilationFailed(result.asExitCode, executionStrategy)
        } catch (e: FailedCompilationException) {
            backup?.tryRestoringOnRecoverableException(e) { restoreAction ->
                log.info(DEFAULT_BACKUP_RESTORE_MESSAGE)
                restoreAction()
            }
            throw e
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
