/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalDecoratedKotlinCompilation

class PrototypeAndroidCompilation(delegate: Delegate) : ExternalDecoratedKotlinCompilation(delegate) {
    override val kotlinOptions: KotlinCommonOptions
        get() = super.kotlinOptions as KotlinJvmOptions

    @Suppress("UNCHECKED_CAST")
    override val compilerOptions: HasCompilerOptions<KotlinJvmCompilerOptions>
        get() = super.compilerOptions as HasCompilerOptions<KotlinJvmCompilerOptions>

    var androidCompilationSpecificStuff = 10
}

