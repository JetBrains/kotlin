/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.Task
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner
import org.jetbrains.kotlin.compilerRunner.KotlinNativeToolRunner
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner

internal fun KotlinNativeCInteropRunner.Companion.createExecutionContext(
    task: Task,
    isInIdeaSync: Boolean,
    runnerSettings: KotlinNativeToolRunner.Settings,
    gradleExecutionContext: KotlinToolRunner.GradleExecutionContext
): KotlinNativeCInteropRunner.ExecutionContext {
    return if (isInIdeaSync) IdeaSyncKotlinNativeCInteropRunnerExecutionContext(runnerSettings, gradleExecutionContext, task)
    else DefaultKotlinNativeCInteropRunnerExecutionContext(runnerSettings, gradleExecutionContext)
}

private class DefaultKotlinNativeCInteropRunnerExecutionContext(
    override val runnerSettings: KotlinNativeToolRunner.Settings,
    override val gradleExecutionContext: KotlinToolRunner.GradleExecutionContext
) : KotlinNativeCInteropRunner.ExecutionContext {
    override fun runWithContext(action: () -> Unit) = action()
}

private class IdeaSyncKotlinNativeCInteropRunnerExecutionContext(
    override val runnerSettings: KotlinNativeToolRunner.Settings,
    override val gradleExecutionContext: KotlinToolRunner.GradleExecutionContext,
    private val task: Task
) : KotlinNativeCInteropRunner.ExecutionContext {

    override fun runWithContext(action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
            task.logger.warn("Warning: Failed to generate cinterop for ${task.path}: ${t.message ?: ""}", t)
            task.outputs.files.forEach { file -> file.deleteRecursively() }
        }
    }
}
