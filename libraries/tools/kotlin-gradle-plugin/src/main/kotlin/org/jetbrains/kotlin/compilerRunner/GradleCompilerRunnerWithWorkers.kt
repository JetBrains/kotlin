/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.GradleException
import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import java.io.File
import javax.inject.Inject

/**
 * Uses Gradle worker api to run kotlin compilation.
 */
internal class GradleCompilerRunnerWithWorkers(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    kotlinDaemonJvmArgs: List<String>?,
    buildMetrics: BuildMetricsReporter,
    compilerExecutionStrategy: KotlinCompilerExecutionStrategy,
    private val workerExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider, jdkToolsJar, kotlinDaemonJvmArgs, buildMetrics, compilerExecutionStrategy) {
    override fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue {

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(GradleKotlinCompilerWorkAction::class.java) { params ->
            params.compilerWorkArguments.set(workArgs)
            if (taskOutputsBackup != null) {
                params.taskOutputs.from(taskOutputsBackup.outputs)
                params.buildDir.set(taskOutputsBackup.buildDirectory)
                params.snapshotsDir.set(taskOutputsBackup.snapshotsDir)
                params.metricsReporter.set(buildMetrics)
            }
        }
        return workQueue
    }

    internal abstract class GradleKotlinCompilerWorkAction @Inject constructor(
        private val fileSystemOperations: FileSystemOperations
    ) : WorkAction<GradleKotlinCompilerWorkParameters> {

        private val logger = Logging.getLogger("kotlin-compile-worker")

        override fun execute() {
            val taskOutputsBackup = if (parameters.snapshotsDir.isPresent) {
                TaskOutputsBackup(
                    fileSystemOperations,
                    parameters.buildDir,
                    parameters.snapshotsDir,
                    parameters.taskOutputs
                )
            } else {
                null
            }

            try {
                GradleKotlinCompilerWork(
                    parameters.compilerWorkArguments.get()
                ).run()
            } catch (e: GradleException) {
                // Currently, metrics are not reported as in the worker we are getting new instance of [BuildMetricsReporter]
                // [BuildDataRecorder] knows nothing about this new instance. Possibly could be fixed in the future by migrating
                // [BuildMetricsReporter] to be shared Gradle service.
                if (taskOutputsBackup != null) {
                    parameters.metricsReporter.get().measure(BuildTime.RESTORE_OUTPUT_FROM_BACKUP) {
                        logger.info("Restoring task outputs to pre-compilation state")
                        taskOutputsBackup.restoreOutputs()
                    }
                }

                throw e
            } finally {
                taskOutputsBackup?.deleteSnapshot()
            }
        }
    }

    internal interface GradleKotlinCompilerWorkParameters : WorkParameters {
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputs: ConfigurableFileCollection
        val snapshotsDir: DirectoryProperty
        val buildDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter>
    }
}