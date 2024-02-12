/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder

/**
 * Wires compilations compiler options into source sets [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.languageSettings].
 */
internal object KotlinCompilationLanguageSettingsConfigurator : KotlinCompilationImplFactory.PreConfigure {
    override fun configure(compilation: KotlinCompilationImpl) {
        // Ignoring non-klib common compilation as it reuses defaultSourceSet with klib common compilation
        if (compilation.platformType == KotlinPlatformType.common &&
            compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME
        ) return

        // Ignoring jsLegacy as it shares a source set with jsIR
        @Suppress("DEPRECATION")
        if (compilation.platformType == KotlinPlatformType.js &&
            compilation.target is org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
        ) return

        val languageSettingsBuilder = (compilation.defaultSourceSet.languageSettings as DefaultLanguageSettingsBuilder)
        if (languageSettingsBuilder.compilationCompilerOptions.isCompleted) {
            compilation.target.project.logger.warn(
                "Compiler options for compilation ${compilation.name} default source set ${compilation.defaultSourceSet.name} " +
                        "'languageSettings' was already set!"
            )
            return
        }

        @Suppress("DEPRECATION")
        languageSettingsBuilder
            .compilationCompilerOptions
            .complete(compilation.compilerOptions.options)
    }
}
