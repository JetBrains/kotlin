/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToCompilerOptions
import org.jetbrains.kotlin.project.model.LanguageSettings

class NativeCompilerOptions(project: Project) : HasCompilerOptions<KotlinCommonCompilerOptions> {

    override val options: KotlinCommonCompilerOptions = project.objects
        .newInstance(KotlinCommonCompilerOptionsDefault::class.java)
        .apply {
            useK2.finalizeValue()
        }

    internal fun syncLanguageSettings(languageSettings: LanguageSettings) {
        applyLanguageSettingsToCompilerOptions(languageSettings, options)
    }
}
