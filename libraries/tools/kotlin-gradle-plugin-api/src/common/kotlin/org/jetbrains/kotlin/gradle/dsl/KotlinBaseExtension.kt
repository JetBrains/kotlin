/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.provider.Property
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
     * When unset, return value checker mode depends on the current Kotlin language version.
     * Until Kotlin `2.5`, the checker is disabled by default, and starting from Kotlin `2.5` it is enabled
     * in the check mode.
     *
     * Unless [returnValueCheckerModeForTests] is set explicitly, this mode is also used for test compilations.
     *
     * Default: `null`
     *
     * @since 2.5.0
     */
    @ExperimentalKotlinGradlePluginApi
    val returnValueCheckerMode: Property<ReturnValueCheckerMode>

    /**
     * Configures the return value checker mode for all test compilations in the project.
     *
     * When unset, test compilations use [returnValueCheckerMode].
     *
     * Default: `null`
     *
     * @since 2.5.0
     */
    @ExperimentalKotlinGradlePluginApi
    val returnValueCheckerModeForTests: Property<ReturnValueCheckerMode>
}
