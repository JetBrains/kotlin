/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptionsAdapter
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.baseModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.moduleNameForCompilation
import org.jetbrains.kotlin.gradle.utils.configureExperimentalTryK2
import org.jetbrains.kotlin.gradle.utils.klibModuleName

internal object KotlinMultiplatformCommonCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = HasCompilerOptionsAdapter.CommonAdapter(
            target.project.objects
                .newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
                .configureExperimentalTryK2(target.project)
        )

        val kotlinOptions = object : KotlinCommonOptions {
            override val options: KotlinCommonCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinNativeCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = HasCompilerOptionsAdapter.NativeAdapter(
            target.project.objects
                .newInstance(KotlinNativeCompilerOptionsDefault::class.java)
                .configureExperimentalTryK2(target.project)
        )

        compilerOptions.options.moduleName.convention(
            target.project.klibModuleName(
                moduleNameForCompilation(
                    compilationName,
                    target.project.baseModuleName()
                )
            )
        )

        val kotlinOptions = object : KotlinCommonOptions {
            override val options get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


internal object KotlinJsCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = HasCompilerOptionsAdapter.JsAdapter(
            target.project.objects
                .newInstance(KotlinJsCompilerOptionsDefault::class.java)
                .configureExperimentalTryK2(target.project)
        )

        val kotlinOptions = object : KotlinJsOptions {
            override val options: KotlinJsCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinJvmCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = HasCompilerOptionsAdapter.JvmAdapter(
            target.project.objects
                .newInstance(KotlinJvmCompilerOptionsDefault::class.java)
                .configureExperimentalTryK2(target.project)
        )

        val kotlinOptions = object : KotlinJvmOptions {
            override val options: KotlinJvmCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


