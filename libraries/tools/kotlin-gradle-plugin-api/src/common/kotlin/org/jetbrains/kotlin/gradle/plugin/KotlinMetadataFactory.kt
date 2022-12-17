/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinMetadataCompileTask

interface KotlinMetadataFactory : KotlinFactory {
    fun createCompilerMultiplatformCommonOptions(): KotlinMultiplatformCommonCompilerOptions

    /** Creates a Kotlin compile task. */
    fun registerKotlinMetadataCompileTask(taskName: String): TaskProvider<out KotlinMetadataCompileTask>
}