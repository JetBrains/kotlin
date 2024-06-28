/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.testUtils

import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

fun BaseKotlinCompile.composeOptions() = pluginOptions.get()
    .flatMap { compilerPluginConfig ->
        compilerPluginConfig.allOptions().filter { it.key == "androidx.compose.compiler.plugins.kotlin" }.values
    }
    .flatten()
    .map { it.key to it.value }

fun KotlinNativeCompile.composeOptions() = compilerPluginOptions
    .allOptions()
    .filter { it.key == "androidx.compose.compiler.plugins.kotlin" }.values
    .flatten()
    .map { it.key to it.value }
