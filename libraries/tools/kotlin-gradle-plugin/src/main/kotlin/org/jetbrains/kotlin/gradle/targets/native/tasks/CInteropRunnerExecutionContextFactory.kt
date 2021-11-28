/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync

internal fun KotlinNativeCInteropRunner.Companion.createExecutionContext(
    task: Task
): KotlinNativeCInteropRunner.ExecutionContext {
    return if (task.project.isInIdeaSync) IdeaSyncKotlinNativeCInteropRunnerExecutionContext(task)
    else DefaultKotlinNativeCInteropRunnerExecutionContext(task.project)
}

private class DefaultKotlinNativeCInteropRunnerExecutionContext(
    override val project: Project
) : KotlinNativeCInteropRunner.ExecutionContext {
    override fun runWithContext(action: () -> Unit) = action()
}

private class IdeaSyncKotlinNativeCInteropRunnerExecutionContext(
    private val task: Task
) : KotlinNativeCInteropRunner.ExecutionContext {

    override val project: Project = task.project

    override fun runWithContext(action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
            task.logger.warn("Warning: Failed to generate cinterop for ${task.path}: ${t.message ?: ""}", t)
            task.outputs.files.forEach { file -> file.deleteRecursively() }
        }
    }
}
