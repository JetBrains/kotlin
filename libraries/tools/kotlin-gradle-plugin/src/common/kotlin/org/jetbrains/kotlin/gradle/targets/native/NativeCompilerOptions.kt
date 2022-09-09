/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.runOnceAfterEvaluated
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToCompilerOptions
import org.jetbrains.kotlin.project.model.LanguageSettings

internal class NativeCompilerOptions(
    project: Project,
    nativeLanguageSettings: LanguageSettings
) : HasCompilerOptions<CompilerCommonOptions> {
    override val options: CompilerCommonOptions = project.objects
        .newInstance(CompilerCommonOptionsDefault::class.java)
        .apply {
            useK2.finalizeValue()
            project.runOnceAfterEvaluated("apply Kotlin native properties from language settings") {
                applyLanguageSettingsToCompilerOptions(nativeLanguageSettings, this)
            }
        }
}
