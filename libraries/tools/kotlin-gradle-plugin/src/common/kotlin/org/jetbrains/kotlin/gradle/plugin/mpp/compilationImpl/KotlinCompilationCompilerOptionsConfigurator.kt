/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.internal.targetCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.moduleNameForCompilation

internal object KotlinCompilationCompilerOptionsFromTargetConfigurator : KotlinCompilationImplFactory.PostConfigure {
    override fun configure(compilation: DecoratedKotlinCompilation<*>) {
        when (@Suppress("DEPRECATION") val compilationCompilerOptions = compilation.compilerOptions.options) {
            is KotlinJvmCompilerOptions -> compilation.useTargetJvmCompilerOptionsAsConvention(compilationCompilerOptions)
            is KotlinJsCompilerOptions -> compilation.useTargetJsCompilerOptionsAsConvention(compilationCompilerOptions)
            is KotlinNativeCompilerOptions -> {
                if (compilation.target is KotlinMetadataTarget) {
                    // Shared native compilation, for example, 'appleMain'
                    compilation.useTargetCommonCompilerOptionsAsConvention(compilationCompilerOptions)
                } else {
                    compilation.useTargetNativeCompilerOptionsAsConvention(compilationCompilerOptions)
                }
            }
            else -> compilation.useTargetCommonCompilerOptionsAsConvention(compilationCompilerOptions)
        }
    }

    private fun DecoratedKotlinCompilation<*>.useTargetJvmCompilerOptionsAsConvention(
        jvmCompilerOptions: KotlinJvmCompilerOptions
    ) {
        val targetCompilerOptions = requireTargetCompilerOptionsType<KotlinJvmCompilerOptions>()

        KotlinJvmCompilerOptionsHelper.syncOptionsAsConvention(
            targetCompilerOptions,
            jvmCompilerOptions
        )

        jvmCompilerOptions.moduleName.convention(
            moduleNameForCompilation(
                compilationName,
                targetCompilerOptions.moduleName
            ).orElse(moduleNameForCompilation())
        )
    }

    private fun DecoratedKotlinCompilation<*>.useTargetJsCompilerOptionsAsConvention(
        jsCompilerOptions: KotlinJsCompilerOptions
    ) {
        // Ignoring legacy JS target
        if (target is KotlinWithJavaTarget<*, *>) return

        val targetCompilerOptions = requireTargetCompilerOptionsType<KotlinJsCompilerOptions>()

        KotlinJsCompilerOptionsHelper.syncOptionsAsConvention(
            targetCompilerOptions,
            jsCompilerOptions
        )

        jsCompilerOptions.moduleName.convention(
            moduleNameForCompilation(
                compilationName,
                targetCompilerOptions.moduleName
            ).orElse(moduleNameForCompilation())
        )
    }

    private fun DecoratedKotlinCompilation<*>.useTargetNativeCompilerOptionsAsConvention(
        nativeCompilerOptions: KotlinNativeCompilerOptions
    ) {
        val targetCompilerOptions = requireTargetCompilerOptionsType<KotlinNativeCompilerOptions>()

        KotlinNativeCompilerOptionsHelper.syncOptionsAsConvention(
            targetCompilerOptions,
            nativeCompilerOptions
        )

        nativeCompilerOptions.moduleName.convention(
            moduleNameForCompilation(
                compilationName,
                targetCompilerOptions.moduleName
            )
        )
    }

    private fun DecoratedKotlinCompilation<*>.useTargetCommonCompilerOptionsAsConvention(
        commonCompilerOptions: KotlinCommonCompilerOptions
    ) {
        KotlinCommonCompilerOptionsHelper.syncOptionsAsConvention(
            target.targetCompilerOptions,
            commonCompilerOptions
        )
    }

    private inline fun <reified T : KotlinCommonCompilerOptions> DecoratedKotlinCompilation<*>.requireTargetCompilerOptionsType(): T {
        val targetCompilerOptions = compilation.target.targetCompilerOptions
        require(targetCompilerOptions is T) {
            "${compilation.compilationName} target ${compilation.target.name}:${compilation.target::class.qualifiedName} has incorrect 'compilerOptions' type " +
                    "${targetCompilerOptions::class.qualifiedName}"
        }

        return targetCompilerOptions
    }
}
