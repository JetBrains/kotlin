/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class GradleKpmNativeVariantCompilationData(
    val variant: GradleKpmNativeVariantInternal
) : GradleKpmVariantCompilationDataInternal<KotlinCommonOptions>, GradleKpmNativeCompilationData<KotlinCommonOptions> {
    override val konanTarget: KonanTarget
        get() = variant.konanTarget


    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(
        "Please declare explicit dependency on kotlinx-cli. This option has no longer effect since 1.9.0",
        level = DeprecationLevel.ERROR
    )
    override val enableEndorsedLibs: Boolean
        get() = false

    override val project: Project
        get() = variant.containingModule.project

    override val owner: GradleKpmNativeVariant
        get() = variant

    override val compilerOptions: HasCompilerOptions<KotlinCommonCompilerOptions> = NativeCompilerOptions(project)

    @Suppress("DEPRECATION")
    @Deprecated("Replaced with compilerOptions.options", replaceWith = ReplaceWith("compilerOptions.options"))
    override val kotlinOptions: KotlinCommonOptions = object : KotlinCommonOptions {
        override val options: KotlinCommonCompilerOptions
            get() = compilerOptions.options
    }
}
