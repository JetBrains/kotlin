/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions

/**
 * Internal class that allows phasing out the deprecated [HasCompilerOptions]
 */
@InternalKotlinGradlePluginApi
sealed class HasCompilerOptionsAdapter<CO : KotlinCommonCompilerOptions> : HasCompilerOptions<CO>, KotlinCommonCompilerOptions {

    class CommonAdapter internal constructor(
        override val options: KotlinCommonCompilerOptions,
    ) : HasCompilerOptionsAdapter<KotlinCommonCompilerOptions>(), KotlinCommonCompilerOptions by options

    class JvmAdapter internal constructor(
        override val options: KotlinJvmCompilerOptions,
    ) : HasCompilerOptionsAdapter<KotlinJvmCompilerOptions>(), KotlinJvmCompilerOptions by options

    class NativeAdapter internal constructor(
        override val options: KotlinNativeCompilerOptions,
    ) : HasCompilerOptionsAdapter<KotlinNativeCompilerOptions>(), KotlinNativeCompilerOptions by options

    class JsAdapter internal constructor(
        override val options: KotlinJsCompilerOptions,
    ) : HasCompilerOptionsAdapter<KotlinJsCompilerOptions>(), KotlinJsCompilerOptions by options

}