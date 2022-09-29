/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions

internal object NativeCompilerOptionsFactory : KotlinCompilationImplFactory.CompilerOptionsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.CompilerOptionsFactory.Options {
        val compilerOptions = NativeCompilerOptions(target.project)

        val kotlinOptions = object : KotlinCommonOptions {
            override val options get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.CompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}