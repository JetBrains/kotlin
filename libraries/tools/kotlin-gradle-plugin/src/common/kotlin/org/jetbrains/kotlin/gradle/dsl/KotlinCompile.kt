/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinToolTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile as KotlinJvmCompileApi

@Suppress("DEPRECATION")
interface KotlinJsCompile : KotlinCompile<KotlinJsOptions>,
    KotlinCompilationTask<KotlinJsCompilerOptions>

@Deprecated(
    message = "Moved into API artifact",
    replaceWith = ReplaceWith("KotlinJvmCompile", "org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile")
)
interface KotlinJvmCompile : KotlinJvmCompileApi

@Suppress("DEPRECATION")
internal interface KotlinNativeCompileTask : KotlinCompile<KotlinCommonOptions>,
    KotlinCompilationTask<KotlinNativeCompilerOptions>

@Suppress("DEPRECATION")
interface KotlinCommonCompile : KotlinCompile<KotlinMultiplatformCommonOptions>,
    KotlinCompilationTask<KotlinMultiplatformCommonCompilerOptions>

@Suppress("DEPRECATION_ERROR")
@Deprecated(KOTLIN_JS_DCE_TOOL_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
interface KotlinJsDce : Task, KotlinToolTask<KotlinJsDceCompilerToolOptions> {

    @Deprecated(KOTLIN_OPTIONS_AS_TOOLS_DEPRECATION_MESSAGE)
    @get:Internal
    val dceOptions: KotlinJsDceOptions

    @Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
    @Deprecated(KOTLIN_OPTIONS_AS_TOOLS_DEPRECATION_MESSAGE)
    fun dceOptions(fn: KotlinJsDceOptions.() -> Unit) {
        dceOptions.fn()
    }

    @get:Input
    val keep: MutableList<String>

    fun keep(vararg fqn: String)
}