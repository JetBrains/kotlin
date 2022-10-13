/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Temporary workaround for external plugins that tries to set up freeCompilerArgs
 * in task execution phase.
 */
@Suppress("DEPRECATION")
class KotlinJvmOptionsCompat(
    private val task: () -> KotlinCompile,
    override val options: KotlinJvmCompilerOptions
) : KotlinJvmOptions {
    override var freeCompilerArgs: List<String>
        get() = if (isTaskExecuting) {
            options.freeCompilerArgs.get().union(task().additionalFreeCompilerArgs).toList()
        } else {
            options.freeCompilerArgs.get()
        }

        set(value) = if (isTaskExecuting) {
            task().logger.warn(
                "kotlinOptions.freeCompilerArgs were changed on task execution phase: ${value.joinToString()}\n" +
                        "This behaviour is deprecated and become an error in future releases!"
            )
            task().additionalFreeCompilerArgs = value
        } else {
            options.freeCompilerArgs.set(value)
        }

    private val isTaskExecuting: Boolean
        get() = task().state.executing
}
