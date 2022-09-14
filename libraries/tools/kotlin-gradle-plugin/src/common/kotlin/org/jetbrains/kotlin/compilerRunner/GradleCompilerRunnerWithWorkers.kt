/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.File
import javax.inject.Inject

/**
 * Uses Gradle worker api to run kotlin compilation.
 */
internal class GradleCompilerRunnerWithWorkers(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    compilerExecutionSettings: CompilerExecutionSettings,
    buildMetrics: BuildMetricsReporter,
    private val workerExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider, jdkToolsJar, compilerExecutionSettings, buildMetrics) {
    override fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue {

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(GradleKotlinCompilerWorkAction::class.java) { params ->
            params.compilerWorkArguments.set(workArgs)
            if (taskOutputsBackup != null) {
                params.taskOutputsToRestore.set(taskOutputsBackup.outputsToRestore)
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
                    parameters.taskOutputsToRestore.get(),
                    logger
                )
            } else {
                null
            }

            try {
                GradleKotlinCompilerWork(
                    parameters.compilerWorkArguments.get()
                ).run()
            } catch (e: FailedCompilationException) {
                // Restore outputs only in cases where we expect that the user will make some changes to their project:
                //   - For a compilation error, the user will need to fix their source code
                //   - For an OOM error, the user will need to increase their memory settings
                // In the other cases where there is nothing the user can fix in their project, we should not restore the outputs.
                // Otherwise, the next build(s) will likely fail in exactly the same way as this build because their inputs and outputs are
                // the same.
                if (taskOutputsBackup != null && (e is CompilationErrorException || e is OOMErrorException)) {
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
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val buildDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter>
    }
}