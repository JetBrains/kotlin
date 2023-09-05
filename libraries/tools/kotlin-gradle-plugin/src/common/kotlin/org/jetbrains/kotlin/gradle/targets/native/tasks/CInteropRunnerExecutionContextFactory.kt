/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner
import org.jetbrains.kotlin.compilerRunner.KotlinNativeToolRunner
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty

internal fun KotlinNativeCInteropRunner.Companion.createExecutionContext(
    task: CInteropProcess,
    isInIdeaSync: Boolean,
    runnerSettings: KotlinNativeToolRunner.Settings,
    gradleExecutionContext: KotlinToolRunner.GradleExecutionContext,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
): KotlinNativeCInteropRunner.ExecutionContext {
    return if (isInIdeaSync) IdeaSyncKotlinNativeCInteropRunnerExecutionContext(runnerSettings, gradleExecutionContext, task, metricsReporter)
    else DefaultKotlinNativeCInteropRunnerExecutionContext(runnerSettings, gradleExecutionContext, task, metricsReporter)
}

private class DefaultKotlinNativeCInteropRunnerExecutionContext(
    override val runnerSettings: KotlinNativeToolRunner.Settings,
    override val gradleExecutionContext: KotlinToolRunner.GradleExecutionContext,
    private val task: CInteropProcess,
    override val metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : KotlinNativeCInteropRunner.ExecutionContext {
    override fun runWithContext(action: () -> Unit) {
        task.errorFileProvider.get().delete()
        action()
    }
}

private class IdeaSyncKotlinNativeCInteropRunnerExecutionContext(
    override val runnerSettings: KotlinNativeToolRunner.Settings,
    override val gradleExecutionContext: KotlinToolRunner.GradleExecutionContext,
    private val task: CInteropProcess,
    override val metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : KotlinNativeCInteropRunner.ExecutionContext {

    override fun runWithContext(action: () -> Unit) {
        val errorFile = task.errorFileProvider.get()
        errorFile.delete()
        try {
            action()
        } catch (t: Throwable) {
            val errorText = "Warning: Failed to generate cinterop for ${task.path}: ${t.message ?: ""}"
            task.logger.warn(errorText, t)
            task.outputs.files.forEach { file -> file.deleteRecursively() }
            errorFile.writeText(errorText)
        }
    }
}
