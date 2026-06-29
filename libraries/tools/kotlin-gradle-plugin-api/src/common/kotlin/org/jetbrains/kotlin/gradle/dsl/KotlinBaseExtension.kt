/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * A plugin DSL extension for configuring common options for the entire project.
 *
 * Use the extension in your build script in the `kotlin` block:
 * ```kotlin
 * kotlin {
 *    // Your extension configuration
 * }
 * ```
 *
 * @since 2.1.0
 */
@Suppress("DEPRECATION")
@KotlinGradlePluginDsl
interface KotlinBaseExtension : KotlinTopLevelExtension {

    /**
     * Configures the return value checker mode for all production compilations in the project.
     *
     * Unless [returnValueCheckerModeForTests] is set explicitly, this mode is also used for test compilations.
     *
     * Default: `null`
     */
    @ExperimentalKotlinGradlePluginApi
    var returnValueCheckerMode: ReturnValueCheckerMode?

    /**
     * Configures the return value checker mode for all test compilations in the project.
     *
     * When `null`, test compilations use [returnValueCheckerMode].
     *
     * Default: `null`
     */
    @ExperimentalKotlinGradlePluginApi
    var returnValueCheckerModeForTests: ReturnValueCheckerMode?

    /**
     * Enables the return value checker in [ReturnValueCheckerMode.Check] mode for both production and test compilations.
     *
     * Equivalent to `returnValueChecker(ReturnValueCheckerMode.Check, ReturnValueCheckerMode.Check)`. This zero-argument
     * overload exists so the function can be called from the Groovy DSL, which cannot use Kotlin default parameter values.
     */
    @ExperimentalKotlinGradlePluginApi
    fun returnValueChecker()

    /**
     * Configures the return value checker for the project.
     *
     * By default, the same [mode] is applied to both production and test compilations. Pass [testMode] to use a
     * different mode for test compilations.
     *
     * Note: Kotlin default parameter values are not available from the Groovy DSL or Java. Non-Kotlin build scripts
     * must pass both arguments explicitly (or use the zero-argument [returnValueChecker] overload).
     *
     * @param mode the mode for production compilations (and tests unless [testMode] is set). Defaults to [ReturnValueCheckerMode.Check].
     * @param testMode the mode for test compilations. Defaults to [mode].
     */
    @ExperimentalKotlinGradlePluginApi
    fun returnValueChecker(mode: ReturnValueCheckerMode = ReturnValueCheckerMode.Check, testMode: ReturnValueCheckerMode = mode)
}