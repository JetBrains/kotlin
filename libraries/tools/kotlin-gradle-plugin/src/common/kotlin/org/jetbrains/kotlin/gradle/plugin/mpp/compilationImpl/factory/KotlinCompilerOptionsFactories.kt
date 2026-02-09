/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.baseModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.moduleNameForCompilation
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.utils.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.utils.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.utils.KotlinMultiplatformCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.utils.moduleName

internal object KotlinMultiplatformCommonCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
        val compilerOptions = object : DeprecatedHasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
            override val options: KotlinMultiplatformCommonCompilerOptions = target.project.objects
                .KotlinMultiplatformCommonCompilerOptionsDefault(target.project)
        }

        @Suppress("DEPRECATION_ERROR")
        val kotlinOptions = object : KotlinCommonOptions {
            @OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)
            @Deprecated(
                message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
                level = DeprecationLevel.ERROR,
            )
            override val options: KotlinCommonCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinNativeCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = NativeCompilerOptions(target.project)
        compilerOptions.options.moduleName.convention(
            target.project.moduleName(
                moduleNameForCompilation(
                    compilationName,
                    target.project.baseModuleName()
                )
            )
        )

        @Suppress("DEPRECATION_ERROR")
        val kotlinOptions = object : KotlinCommonOptions {
            @OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)
            @Deprecated(
                message = KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
                level = DeprecationLevel.ERROR,
            )
            override val options get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


internal object KotlinJsCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
        val compilerOptions = object : DeprecatedHasCompilerOptions<KotlinJsCompilerOptions> {
            override val options: KotlinJsCompilerOptions = target.project.objects
                .KotlinJsCompilerOptionsDefault(target.project)
        }

        @Suppress("DEPRECATION_ERROR")
        val kotlinOptions = object : KotlinJsOptions {
            @OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)
            @Deprecated(
                message = org.jetbrains.kotlin.gradle.dsl.KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
                level = DeprecationLevel.ERROR,
            )
            override val options: KotlinJsCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinJvmCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")
        val compilerOptions = object : DeprecatedHasCompilerOptions<KotlinJvmCompilerOptions> {
            @OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)
            @Deprecated(
                message = org.jetbrains.kotlin.gradle.dsl.KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
                level = DeprecationLevel.ERROR,
            )
            override val options: KotlinJvmCompilerOptions = target.project.objects
                .KotlinJvmCompilerOptionsDefault(target.project)
        }

        @Suppress("DEPRECATION_ERROR")
        val kotlinOptions = object : KotlinJvmOptions {
            @OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)
            @Deprecated(
                message = org.jetbrains.kotlin.gradle.dsl.KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
                level = DeprecationLevel.ERROR,
            )
            override val options: KotlinJvmCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


