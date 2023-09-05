/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.project.model

/**
 * Represents most common Kotlin compilation settings for an entity.
 *
 * **Note**: This interface is soft-deprecated.
 * Instead, better to use existing `compilerOptions` DSL.
 *
 * See also [Compiler options DSL documentation](https://kotlinlang.org/docs/gradle-compiler-options.html).
 */
interface LanguageSettings {

    /**
     * Provides source compatibility with the specified version of Kotlin.
     *
     * Possible values: "1.4 (deprecated)", "1.5 (deprecated)", "1.6", "1.7", "1.8", "1.9", "2.0 (experimental)", "2.1 (experimental)".
     *
     * Default value: `null`
     */
    val languageVersion: String?

    /**
     * Allows using declarations only from the specified version of bundled libraries.
     *
     * Possible values: "1.4 (deprecated)", "1.5 (deprecated)", "1.6", "1.7", "1.8", "1.9", "2.0 (experimental)", "2.1 (experimental)".
     *
     * Default value: `null`
     */
    val apiVersion: String?

    /**
     * Enables progressive compiler mode.
     *
     * In this mode, deprecations and bug fixes for unstable code take effect immediately,
     * instead of going through a graceful migration cycle.
     * Code written in progressive mode is backward compatible. However, code written in
     * non-progressive mode may cause compilation errors in progressive mode.
     *
     *  Default value: false
     */
    val progressiveMode: Boolean

    /**
     * @suppress
     */
    val enabledLanguageFeatures: Set<String>

    /**
     * Enables use of any API that requires opt-in with an opt-in requirement marker containing its fully qualified name.
     *
     * Default value: emptyList<String>()
     */
    val optInAnnotationsInUse: Set<String>
}
