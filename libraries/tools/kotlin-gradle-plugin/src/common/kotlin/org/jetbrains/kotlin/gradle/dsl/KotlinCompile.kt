/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile as KotlinJvmCompileApi

@Suppress("DEPRECATION_ERROR", "DEPRECATION")
interface KotlinJsCompile : KotlinCompile<KotlinJsOptions>,
    KotlinCompilationTask<KotlinJsCompilerOptions>

@Deprecated(
    message = "Moved into API artifact. Scheduled for removal in Kotlin 2.3.",
    replaceWith = ReplaceWith("KotlinJvmCompile", "org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile"),
    level = DeprecationLevel.ERROR,
)
interface KotlinJvmCompile : KotlinJvmCompileApi

@Suppress("DEPRECATION_ERROR", "DEPRECATION")
internal interface KotlinNativeCompileTask : KotlinCompile<KotlinCommonOptions>,
    KotlinCompilationTask<KotlinNativeCompilerOptions>

@Suppress("DEPRECATION_ERROR", "DEPRECATION")
interface KotlinCommonCompile : KotlinCompile<KotlinMultiplatformCommonOptions>,
    KotlinCompilationTask<KotlinMultiplatformCommonCompilerOptions>

