/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal object CommonCompilerOptionsFactory : KotlinCompilationImplFactory.CompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.CompilerOptionsFactory.Options {
        val compilerOptions = object : HasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
            override val options: KotlinMultiplatformCommonCompilerOptions =
                target.project.objects.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
        }

        val kotlinOptions = object : KotlinCommonOptions {
            override val options: KotlinCommonCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.CompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}