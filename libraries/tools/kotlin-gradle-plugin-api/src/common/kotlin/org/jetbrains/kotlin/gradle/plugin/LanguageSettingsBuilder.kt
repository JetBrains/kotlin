/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.project.model.LanguageSettings

/**
 * Provides DSL to configure [LanguageSettings] for a [KotlinSourceSet] entity.
 *
 * **Note**: This interface is soft-deprecated.
 * Instead, better to use existing `compilerOptions` DSL.
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
     * Adds additional opt-in requirement marker with the given fully qualified name.
     */
    fun optIn(annotationName: String)
}
