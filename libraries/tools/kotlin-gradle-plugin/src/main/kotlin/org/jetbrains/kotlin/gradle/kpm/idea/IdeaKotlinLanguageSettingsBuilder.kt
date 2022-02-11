/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.project.model.LanguageSettings

internal fun LanguageSettings.toIdeaKotlinLanguageSettings(): IdeaKotlinLanguageSettings {
    return IdeaKotlinLanguageSettingsImpl(
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        isProgressiveMode = progressiveMode,
        enabledLanguageFeatures = enabledLanguageFeatures.toSet(),
        optInAnnotationsInUse = optInAnnotationsInUse.toSet(),
        compilerPluginArguments = (this as? DefaultLanguageSettingsBuilder)?.compilerPluginArguments?.toList().orEmpty(),
        compilerPluginClasspath = (this as? DefaultLanguageSettingsBuilder)?.compilerPluginClasspath?.toList().orEmpty(),
        freeCompilerArgs = (this as? DefaultLanguageSettingsBuilder)?.freeCompilerArgs?.toList().orEmpty()
    )
}
