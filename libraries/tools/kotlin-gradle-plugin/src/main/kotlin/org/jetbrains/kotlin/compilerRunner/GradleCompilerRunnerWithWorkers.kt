/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import java.io.File

/**
 * Uses Gradle worker api to run kotlin compilation.
 */
internal class GradleCompilerRunnerWithWorkers(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    kotlinDaemonJvmArgs: List<String>?,
    buildMetrics: BuildMetricsReporter,
    private val workerExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider, jdkToolsJar, kotlinDaemonJvmArgs, buildMetrics) {
    override fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue {

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(GradleKotlinCompilerWorkAction::class.java) {
            it.compilerWorkArguments.set(workArgs)
            if (taskOutputsBackup != null) {
                it.taskOutputs.from(taskOutputsBackup.outputs)
                it.taskOutputsSnapshot.set(taskOutputsBackup.previousOutputs)
                it.metricsReporter.set(buildMetrics)
            } else {
                // MapProperty has empty value by default: https://github.com/gradle/gradle/issues/7485
                it.taskOutputsSnapshot.set(null as Map<File, Array<Byte>>?)
            }
        }
        return workQueue
    }

    internal abstract class GradleKotlinCompilerWorkAction
        : WorkAction<GradleKotlinCompilerWorkParameters> {

        override fun execute() {
            try {
                GradleKotlinCompilerWork(
                    parameters.compilerWorkArguments.get()
                ).run()
            } catch (e: GradleException) {
                if (parameters.taskOutputsSnapshot.isPresent) {
                    val taskOutputsBackup = TaskOutputsBackup(
                        parameters.taskOutputs,
                        parameters.taskOutputsSnapshot.get()
                    )
                    // Currently, metrics are not reported as in the worker we are getting new instance of [BuildMetricsReporter]
                    // [BuildDataRecorder] knows nothing about this new instance. Possibly could be fixed in the future by migrating
                    // [BuildMetricsReporter] to be shared Gradle service.
                    parameters.metricsReporter.get().measure(BuildTime.RESTORE_OUTPUT_FROM_BACKUP) {
                        taskOutputsBackup.restoreOutputs()
                    }
                }

                throw e
            }
        }
    }

    internal interface GradleKotlinCompilerWorkParameters : WorkParameters {
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputs: ConfigurableFileCollection
        val taskOutputsSnapshot: MapProperty<File, Array<Byte>>
        val metricsReporter: Property<BuildMetricsReporter>
    }
}