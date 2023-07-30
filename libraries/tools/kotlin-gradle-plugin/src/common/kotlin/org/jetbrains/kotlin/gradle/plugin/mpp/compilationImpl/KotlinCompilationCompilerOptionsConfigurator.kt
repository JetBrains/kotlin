/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.moduleNameForCompilation

internal class KotlinCompilationJvmCompilerOptionsFromTargetConfigurator(
    private val targetCompilerOptions: KotlinJvmCompilerOptions
) : KotlinCompilationImplFactory.PostConfigure {
    override fun configure(compilation: DecoratedKotlinCompilation<*>) {
        val jvmCompilerOptions = compilation.compilerOptions.options as KotlinJvmCompilerOptions
        KotlinJvmCompilerOptionsHelper.syncOptionsAsConvention(
            targetCompilerOptions,
            jvmCompilerOptions
        )

        jvmCompilerOptions.moduleName.convention(
            moduleNameForCompilation(
                compilation.compilationName,
                targetCompilerOptions.moduleName
            ).orElse(compilation.moduleNameForCompilation())
        )
    }
}

internal class KotlinCompilationNativeCompilerOptionsFromTargetConfigurator(
    private val targetCompilerOptions: KotlinNativeCompilerOptions
) : KotlinCompilationImplFactory.PostConfigure {
    override fun configure(compilation: DecoratedKotlinCompilation<*>) {
        val nativeCompilerOptions = compilation.compilerOptions.options as KotlinNativeCompilerOptions
        KotlinNativeCompilerOptionsHelper.syncOptionsAsConvention(
            targetCompilerOptions,
            nativeCompilerOptions
        )

        nativeCompilerOptions.moduleName.convention(
            moduleNameForCompilation(
                compilation.compilationName,
                targetCompilerOptions.moduleName
            )
        )
    }
}

internal class KotlinCompilationJsCompilerOptionsFromTargetConfigurator(
    private val targetCompilerOptions: KotlinJsCompilerOptions
) : KotlinCompilationImplFactory.PostConfigure {
    override fun configure(compilation: DecoratedKotlinCompilation<*>) {
        val jsCompilerOptions = compilation.compilerOptions.options as KotlinJsCompilerOptions
        KotlinJsCompilerOptionsHelper.syncOptionsAsConvention(
            targetCompilerOptions,
            jsCompilerOptions
        )

        jsCompilerOptions.moduleName.convention(
            moduleNameForCompilation(
                compilation.compilationName,
                targetCompilerOptions.moduleName
            ).orElse(compilation.moduleNameForCompilation())
        )
    }
}
