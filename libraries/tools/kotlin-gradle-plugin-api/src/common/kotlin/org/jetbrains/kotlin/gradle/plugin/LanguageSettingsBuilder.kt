/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.project.model.LanguageSettings

/**
 * Provides a DSL to configure most common Kotlin compilation settings for an entity.
 *
 * **Note**: This interface is soft-deprecated.
 * Instead, better to use existing `compilerOptions` DSL.
 *
 * See also [Compiler options DSL documentation](https://kotlinlang.org/docs/gradle-compiler-options.html).
 */
interface LanguageSettingsBuilder : LanguageSettings {
    override var languageVersion: String?

    override var apiVersion: String?

    override var progressiveMode: Boolean

    /**
     * @suppress
     */
    fun enableLanguageFeature(name: String)

    override val enabledLanguageFeatures: Set<String>

    /**
     * Opts into a specific [OptIn] annotation.
     *
     * @param annotationName The fully qualified name of the annotation to opt into.
     */
    fun optIn(annotationName: String)
}
