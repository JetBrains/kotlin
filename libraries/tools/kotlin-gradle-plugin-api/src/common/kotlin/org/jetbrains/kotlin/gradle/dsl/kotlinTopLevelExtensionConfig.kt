/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

/**
 * A plugin DSL extension for configuring common options for the entire project.
 *
 * Use the extension in your build script in the `kotlin` block:
 * ```kotlin
 * kotlin {
 *    // Your extension configuration
 * }
 * ```
 */
@KotlinGradlePluginDsl
interface KotlinTopLevelExtensionConfig {

    /**
     * Specifies the version of the core Kotlin libraries that are added to the Kotlin compile classpath,
     * unless there is already a dependency added to this project.
     *
     * The core Kotlin libraries are:
     * - 'kotlin-stdlib'
     * - 'kotlin-test'
     * - 'kotlin-dom-api-compat'
     *
     * Default: The same version as the version used in the Kotlin Gradle plugin
     */
    var coreLibrariesVersion: String

    /**
     * Configures default explicit API mode for all non-test compilations in the project.
     *
     * This mode tells the compiler if and how to report issues on all public API declarations
     * that don't have an explicit visibility or return type.
     *
     * Default: `null`
     */
    var explicitApi: ExplicitApiMode?

    /**
     * Sets [explicitApi] option to report issues as errors.
     */
    fun explicitApi()

    /**
     * Sets [explicitApi] option to report issues as warnings.
     */
    fun explicitApiWarning()
}

/**
 * Different modes that can be used to set the level of issue reporting for [KotlinTopLevelExtensionConfig.explicitApi] option.
 */
enum class ExplicitApiMode(
    /**
     * @suppress
     */
    @Deprecated("Should not be exposed in api", level = DeprecationLevel.ERROR)
    val cliOption: String
) {
    /**
     * Reports API issues as errors.
     */
    Strict("strict"),

    /**
     * Reports API issues as warnings.
     */
    Warning("warning"),

    /**
     * Disables issues reporting.
     */
    Disabled("disable");

    /**
     * @suppress
     */
    @Deprecated("Should not be exposed in api", level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION_ERROR")
    fun toCompilerArg() = "-Xexplicit-api=$cliOption"
}
