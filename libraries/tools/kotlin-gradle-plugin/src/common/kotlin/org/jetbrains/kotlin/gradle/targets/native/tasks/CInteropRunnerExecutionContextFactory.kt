/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner

internal interface CInteropProcessExecutionContext {
    val task: CInteropProcess
    val cinteropRunner: KotlinNativeToolRunner
    fun runWithContext(action: KotlinNativeToolRunner.() -> Unit)
}

internal fun CInteropProcess.createExecutionContext(
    isInIdeaSync: Boolean,
    cinteropRunner: KotlinNativeToolRunner,
): CInteropProcessExecutionContext = if (isInIdeaSync) {
    IdeaSyncKotlinNativeCInteropRunnerExecutionContext(this, cinteropRunner)
} else {
    DefaultKotlinNativeCInteropRunnerExecutionContext(this, cinteropRunner)
}

private class DefaultKotlinNativeCInteropRunnerExecutionContext(
    override val task: CInteropProcess,
    override val cinteropRunner: KotlinNativeToolRunner,
) : CInteropProcessExecutionContext {
    override fun runWithContext(action: KotlinNativeToolRunner.() -> Unit) {
        task.errorFileProvider.get().delete()
        cinteropRunner.action()
    }
}

private class IdeaSyncKotlinNativeCInteropRunnerExecutionContext(
    override val task: CInteropProcess,
    override val cinteropRunner: KotlinNativeToolRunner,
) : CInteropProcessExecutionContext {

    override fun runWithContext(action: KotlinNativeToolRunner.() -> Unit) {
        val errorFile = task.errorFileProvider.get()
        errorFile.delete()
        try {
            cinteropRunner.action()
        } catch (t: Throwable) {
            val errorText = "Warning: Failed to generate cinterop for ${task.path}: ${t.message ?: ""}"
            task.logger.warn(errorText, t)
            task.outputs.files.forEach { file -> file.deleteRecursively() }
            errorFile.writeText(errorText)
        }
    }
}
