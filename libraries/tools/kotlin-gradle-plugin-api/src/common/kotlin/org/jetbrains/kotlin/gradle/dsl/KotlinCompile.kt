/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Represents a Kotlin task compiling using configurable [kotlinOptions].
 *
 * See [KotlinCommonOptions] and its inheritors for possible Kotlin compiler options.
 *
 * **Note**: This interface is soft-deprecated and only exists for compatibility to configure Kotlin compilation options
 * using soft-deprecated [kotlinOptions].
 * Instead, better to use [KotlinCompilationTask] to configure Kotlin compilation options via [KotlinCompilationTask.compilerOptions].
 *
 * @see [KotlinCommonOptions]
 */
@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
@Deprecated(
    message = "Replaced with 'KotlinCompilationTask' that exposes the compiler options DSL. More details are here: https://kotl.in/u1r8ln"
)
@KotlinGradlePluginDsl
interface KotlinCompile<out T : KotlinCommonOptionsDeprecated> : Task {

    /**
     * Represents the compiler options used by a Kotlin compilation process.
     *
     * This can be used to get the values of currently configured options or modify them.
     *
     * The [kotlinOptions] configuration is delegated to the related task `compilerOptions` input configuration.
     */
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE)
    @get:Internal
    val kotlinOptions: T

    /**
     * Configures the [kotlinOptions] with the provided configuration.
     */
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE)
    fun kotlinOptions(fn: T.() -> Unit) {
        @Suppress("DEPRECATION")
        kotlinOptions.fn()
    }

    /**
     * Configures the [kotlinOptions] with the provided configuration.
     */
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE)
    fun kotlinOptions(fn: Action<in T>) {
        @Suppress("DEPRECATION")
        fn.execute(kotlinOptions)
    }
}

/**
 * @suppress
 */
@InternalKotlinGradlePluginApi
const val KOTLIN_OPTIONS_DEPRECATION_MESSAGE = "Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln"

/**
 * @suppress
 */
@InternalKotlinGradlePluginApi
const val KOTLIN_OPTIONS_AS_TOOLS_DEPRECATION_MESSAGE = "Please migrate to the toolOptions DSL. More details are here: https://kotl.in/u1r8ln"
