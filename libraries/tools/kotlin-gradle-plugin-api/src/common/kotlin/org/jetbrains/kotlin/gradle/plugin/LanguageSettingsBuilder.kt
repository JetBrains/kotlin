/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.project.model.LanguageSettings

/**
 * Provides the DSL to configure [LanguageSettings] for a [KotlinSourceSet] entity.
 *
 * **Note**: This interface will be deprecated in the future.
 * Instead, it is better to use the existing `compilerOptions` DSL.
 *
 * For more information, see [Compiler options in the Kotlin Gradle plugin](https://kotlinlang.org/docs/gradle-compiler-options.html).
 */
interface LanguageSettingsBuilder : LanguageSettings {
    override var languageVersion: String?

    override var apiVersion: String?

    override var progressiveMode: Boolean

    /**
     * @suppress
     */
    fun enableLanguageFeature(name: String)

    /**
     * @suppress
     */
    override val enabledLanguageFeatures: Set<String>

    /**
     * Adds an additional opt-in requirement marker with the given fully qualified name.
     *
     * See also [org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions.optIn].
     */
    fun optIn(annotationName: String)
}
