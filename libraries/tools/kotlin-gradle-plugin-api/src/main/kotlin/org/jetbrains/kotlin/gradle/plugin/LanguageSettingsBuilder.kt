/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.project.model.LanguageSettings

interface LanguageSettingsBuilder : LanguageSettings {
    override var languageVersion: String?

    override var apiVersion: String?

    override var progressiveMode: Boolean

    fun enableLanguageFeature(name: String)

    override val enabledLanguageFeatures: Set<String>

    @Deprecated("Unsupported and will be removed in next major releases", replaceWith = ReplaceWith("optIn(name)"))
    fun useExperimentalAnnotation(name: String)

    fun optIn(annotationName: String)

    @Deprecated("Unsupported and will be removed in next major releases", replaceWith = ReplaceWith("optInAnnotationsInUse"))
    override val experimentalAnnotationsInUse: Set<String>
}
