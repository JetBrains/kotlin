/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.KotlinGradlePluginDsl

@KotlinGradlePluginDsl
interface HasCompilerOptions {
    val compilerOptions: KotlinCommonCompilerOptions

    operator fun <T : KotlinCommonCompilerOptions> T.invoke(configuration: T.() -> Unit) {
        apply(configuration)
    }
}
