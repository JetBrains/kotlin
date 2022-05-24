/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.project.model.LanguageSettings

internal fun IdeaKpmLanguageSettings(languageSettings: LanguageSettings): IdeaKpmLanguageSettings {
    return IdeaKpmLanguageSettingsImpl(
        languageVersion = languageSettings.languageVersion,
        apiVersion = languageSettings.apiVersion,
        isProgressiveMode = languageSettings.progressiveMode,
        enabledLanguageFeatures = languageSettings.enabledLanguageFeatures.toSet(),
        optInAnnotationsInUse = languageSettings.optInAnnotationsInUse.toSet(),
        compilerPluginArguments = (languageSettings as? DefaultLanguageSettingsBuilder)?.compilerPluginArguments?.toList().orEmpty(),
        compilerPluginClasspath = (languageSettings as? DefaultLanguageSettingsBuilder)?.compilerPluginClasspath?.toList().orEmpty(),
        freeCompilerArgs = (languageSettings as? DefaultLanguageSettingsBuilder)?.freeCompilerArgs?.toList().orEmpty()
    )
}
