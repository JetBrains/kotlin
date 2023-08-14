/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.compilerRunner.asFinishLogMessage
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.SL4JKotlinLogger
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdService
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.throwExceptionIfCompilationFailed
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.slf4j.LoggerFactory
import java.io.File

internal abstract class BuildToolsApiCompilationWork : WorkAction<BuildToolsApiCompilationWork.BuildToolsApiCompilationParameters> {
    internal interface BuildToolsApiCompilationParameters : WorkParameters {
        val buildIdService: Property<BuildIdService>
        val buildFinishedListenerService: Property<BuildFinishedListenerService>
        val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val buildDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>
    }

    private val workArguments
        get() = parameters.compilerWorkArguments.get()

    private val taskPath
        get() = workArguments.taskPath

    private val log: KotlinLogger by lazy(LazyThreadSafetyMode.NONE) {
        TaskLoggers.get(taskPath)?.let { GradleKotlinLogger(it).apply { debug("Using '$taskPath' logger") } }
            ?: run {
                val logger = LoggerFactory.getLogger("GradleKotlinCompilerWork")
                val kotlinLogger = if (logger is org.gradle.api.logging.Logger) {
                    GradleKotlinLogger(logger)
                } else SL4JKotlinLogger(logger)

                kotlinLogger.apply {
                    debug("Could not get logger for '$taskPath'. Falling back to sl4j logger")
                }
            }
    }

    override fun execute() {
        val executionStrategy = workArguments.compilerExecutionSettings.strategy
        try {
            val classLoader = parameters.classLoadersCachingService.get()
                .getClassLoader(workArguments.compilerFullClasspath, SharedApiClassesClassLoaderProvider)
            val compilationService = CompilationService.loadImplementation(classLoader)
            val buildId = ProjectId.ProjectUUID(parameters.buildIdService.get().buildId)
            parameters.buildFinishedListenerService.get().onCloseOnceByKey(buildId.toString()) {
                compilationService.finishProjectCompilation(buildId)
            }
            val executionConfig = compilationService.makeCompilerExecutionStrategyConfiguration().apply {
                when (executionStrategy) {
                    KotlinCompilerExecutionStrategy.DAEMON -> useDaemonStrategy(workArguments.compilerExecutionSettings.daemonJvmArgs ?: emptyList())
                    KotlinCompilerExecutionStrategy.IN_PROCESS -> useInProcessStrategy()
                    else -> error("The \"$executionStrategy\" execution strategy is not supported by the Build Tools API")
                }
            }
            val jvmCompilationConfig = compilationService.makeJvmCompilationConfiguration()
                .useLogger(log)
                .useKotlinScriptFilenameExtensions(workArguments.kotlinScriptExtensions.toList())
            val icEnv = workArguments.incrementalCompilationEnvironment
            val classpathChanges = icEnv?.classpathChanges
            if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled) {
                val classpathSnapshotsConfig = jvmCompilationConfig.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
                    .setRootProjectDir(icEnv.rootProjectDir)
                    .setBuildDir(icEnv.buildDir)
                    .usePreciseJavaTracking(icEnv.usePreciseJavaTracking)
                    .usePreciseCompilationResultsBackup(icEnv.preciseCompilationResultsBackup)
                    .keepIncrementalCompilationCachesInMemory(icEnv.keepIncrementalCompilationCachesInMemory)
                    .useOutputDirs(workArguments.outputFiles)
                    .forceNonIncrementalMode(classpathChanges !is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun)
                val classpathSnapshotsParameters = ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
                    classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles,
                    classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile,
                )
                when (classpathChanges) {
                    is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges -> classpathSnapshotsConfig.assureNoClasspathSnapshotsChanges()
                    is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun -> classpathSnapshotsConfig.forceNonIncrementalMode()
                    else -> {}
                }
                jvmCompilationConfig.useIncrementalCompilation(
                    icEnv.workingDir,
                    icEnv.changedFiles,
                    classpathSnapshotsParameters,
                    classpathSnapshotsConfig,
                )
            }
            val result = compilationService.compileJvm(
                buildId,
                executionConfig,
                jvmCompilationConfig,
                emptyList(),
                workArguments.compilerArgs.toList(),
            )
            throwExceptionIfCompilationFailed(result.asExitCode, executionStrategy)
        } finally {
            log.info(executionStrategy.asFinishLogMessage)
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