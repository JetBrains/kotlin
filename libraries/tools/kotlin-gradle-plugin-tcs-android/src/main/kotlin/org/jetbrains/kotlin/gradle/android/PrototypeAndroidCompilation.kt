/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinCompilation

class PrototypeAndroidCompilation(delegate: Delegate) : DecoratedExternalKotlinCompilation(delegate) {
    override val kotlinOptions: KotlinCommonOptions
        get() = super.kotlinOptions as KotlinJvmOptions

    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("UNCHECKED_CAST")
    override val compilerOptions: HasCompilerOptions<KotlinJvmCompilerOptions>
        get() = super.compilerOptions as HasCompilerOptions<KotlinJvmCompilerOptions>

    var androidCompilationSpecificStuff = 10
}

