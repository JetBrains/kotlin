/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompileTask

interface KotlinNativeFactory : KotlinFactory {
    /** Creates a Kotlin compile task. */
    fun registerKotlinNativeCompileTask(
        taskName: String,
        targetName: String,
        compilationName: String
    ): TaskProvider<out KotlinNativeCompileTask>
}