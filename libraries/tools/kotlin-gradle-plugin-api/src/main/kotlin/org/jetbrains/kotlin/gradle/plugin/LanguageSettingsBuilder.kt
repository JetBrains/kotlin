/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

interface LanguageSettingsBuilder {
    var languageVersion: String?

    var apiVersion: String?

    var progressiveMode: Boolean

    fun enableLanguageFeature(name: String)

    val enabledLanguageFeatures: Set<String>

    fun useExperimentalAnnotation(name: String)

    val experimentalAnnotationsInUse: Set<String>
}