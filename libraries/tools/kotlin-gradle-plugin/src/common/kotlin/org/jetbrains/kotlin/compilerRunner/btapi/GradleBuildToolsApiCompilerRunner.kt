/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.gradle.api.provider.Provider
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.compilerRunner.CompilerExecutionSettings
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.GradleKotlinCompilerWorkArguments
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdService
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import java.io.File

internal class GradleBuildToolsApiCompilerRunner(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    compilerExecutionSettings: CompilerExecutionSettings,
    buildMetrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    private val workerExecutor: WorkerExecutor,
    private val cachedClassLoadersService: Provider<ClassLoadersCachingBuildService>,
    private val buildFinishedListenerService: Provider<BuildFinishedListenerService>,
    private val buildIdService: Provider<BuildIdService>,
    fusMetricsConsumer: StatisticsValuesConsumer?,
) : GradleCompilerRunner(taskProvider, jdkToolsJar, compilerExecutionSettings, buildMetrics, fusMetricsConsumer) {


    override fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue {
        buildMetrics.addTimeMetric(GradleBuildPerformanceMetric.CALL_WORKER)
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(BuildToolsApiCompilationWork::class.java) { params ->
            params.compilerWorkArguments.set(workArgs)
            params.classLoadersCachingService.set(cachedClassLoadersService)
            params.buildFinishedListenerService.set(buildFinishedListenerService)
            params.buildIdService.set(buildIdService)
            if (taskOutputsBackup != null) {
                params.taskOutputsToRestore.set(taskOutputsBackup.outputsToRestore)
                params.buildDir.set(taskOutputsBackup.buildDirectory)
                params.snapshotsDir.set(taskOutputsBackup.snapshotsDir)
                params.metricsReporter.set(buildMetrics)
            }
        }
        return workQueue
    }
}