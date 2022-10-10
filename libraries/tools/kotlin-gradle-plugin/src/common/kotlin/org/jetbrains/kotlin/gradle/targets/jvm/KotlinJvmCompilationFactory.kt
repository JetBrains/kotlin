/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

open class KotlinJvmCompilationFactory(
    override val target: KotlinJvmTarget
) : KotlinCompilationFactory<KotlinJvmCompilation> {
    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    @Suppress("DEPRECATION")
    override fun create(name: String): KotlinJvmCompilation =
        target.project.objects.newInstance(
            KotlinJvmCompilation::class.java,
            DefaultCompilationDetailsWithRuntime<KotlinJvmOptions, KotlinJvmCompilerOptions>(
                target,
                name,
                getOrCreateDefaultSourceSet(name),
                {
                    object : HasCompilerOptions<KotlinJvmCompilerOptions> {
                        override val options: KotlinJvmCompilerOptions =
                            target.project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)
                    }
                },
                {
                    object : KotlinJvmOptions {
                        override val options: KotlinJvmCompilerOptions
                            get() = compilerOptions.options
                    }
                }
            )
        )
}