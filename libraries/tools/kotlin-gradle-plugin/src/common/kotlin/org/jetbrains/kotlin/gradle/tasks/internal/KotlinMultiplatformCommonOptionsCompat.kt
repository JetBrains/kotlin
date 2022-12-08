/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

@Suppress("DEPRECATION")
class KotlinMultiplatformCommonOptionsCompat(
    private val task: () -> KotlinCompileCommon,
    override val options: KotlinMultiplatformCommonCompilerOptions
) : KotlinMultiplatformCommonOptions {

    override var freeCompilerArgs: List<String>
        get() {
            val executionTimeFreeCompilerArgs = task().executionTimeFreeCompilerArgs
            return if (isTaskExecuting && executionTimeFreeCompilerArgs != null) {
                executionTimeFreeCompilerArgs
            } else {
                options.freeCompilerArgs.get()
            }
        }

        set(value) = if (isTaskExecuting) {
            task().nagUserFreeArgsModifiedOnExecution(value)
            task().executionTimeFreeCompilerArgs = value
        } else {
            options.freeCompilerArgs.set(value)
        }

    private val isTaskExecuting: Boolean
        get() = task().state.executing
}