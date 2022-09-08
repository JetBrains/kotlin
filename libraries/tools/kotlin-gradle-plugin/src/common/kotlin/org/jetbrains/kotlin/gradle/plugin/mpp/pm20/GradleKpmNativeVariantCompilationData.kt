/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.runOnceAfterEvaluated
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToCompilerOptions
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class GradleKpmNativeVariantCompilationData(
    val variant: GradleKpmNativeVariantInternal
) : GradleKpmVariantCompilationDataInternal<KotlinCommonOptions>, KotlinNativeCompilationData<KotlinCommonOptions> {
    override val konanTarget: KonanTarget
        get() = variant.konanTarget

    override val enableEndorsedLibs: Boolean
        get() = variant.enableEndorsedLibraries

    override val project: Project
        get() = variant.containingModule.project

    override val owner: GradleKpmNativeVariant
        get() = variant

    override val compilerOptions: HasCompilerOptions<CompilerCommonOptions> =
        object : HasCompilerOptions<CompilerCommonOptions> {
            override val options: CompilerCommonOptions =
                project.objects.newInstance(CompilerCommonOptionsDefault::class.java)
                    .apply {
                        useK2.finalizeValue()
                        project.runOnceAfterEvaluated("apply Kotlin native properties from language settings") {
                            applyLanguageSettingsToCompilerOptions(variant.languageSettings, this)
                        }
                    }
        }

    @Suppress("DEPRECATION")
    @Deprecated("Replaced with compilerOptions.options", replaceWith = ReplaceWith("compilerOptions.options"))
    override val kotlinOptions: KotlinCommonOptions = object : KotlinCommonOptions {
        override val options: CompilerCommonOptions
            get() = compilerOptions.options
    }
}
