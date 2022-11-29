/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions

internal object KotlinMultiplatformCommonCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = object : HasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
            override val options: KotlinMultiplatformCommonCompilerOptions =
                target.project.objects.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
        }

        val kotlinOptions = object : KotlinCommonOptions {
            override val options: KotlinCommonCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinNativeCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = NativeCompilerOptions(target.project)

        val kotlinOptions = object : KotlinCommonOptions {
            override val options get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


internal object KotlinJsCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = object : HasCompilerOptions<KotlinJsCompilerOptions> {
            override val options: KotlinJsCompilerOptions =
                target.project.objects.newInstance(KotlinJsCompilerOptionsDefault::class.java)
        }

        val kotlinOptions = object : KotlinJsOptions {
            override val options: KotlinJsCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinJvmCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = object : HasCompilerOptions<KotlinJvmCompilerOptions> {
            override val options: KotlinJvmCompilerOptions =
                target.project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)
        }

        val kotlinOptions = object : KotlinJvmOptions {
            override val options: KotlinJvmCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


