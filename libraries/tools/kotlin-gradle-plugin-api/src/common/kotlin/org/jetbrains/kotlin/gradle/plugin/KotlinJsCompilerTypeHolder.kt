/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface KotlinJsCompilerTypeHolder {
    @Deprecated("Because only IR compiler is left, no more necessary to know about compiler type in properties")
    val compilerTypeFromProperties: KotlinJsCompilerType?

    val defaultJsCompilerType: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    // Necessary to get rid of KotlinJsCompilerType import in build script
    @Deprecated("Legacy compiler is deprecated. Migrate your project to the new IR-based compiler", level = DeprecationLevel.HIDDEN)
    val LEGACY: KotlinJsCompilerType
        get() = KotlinJsCompilerType.LEGACY

    val IR: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    @Deprecated("Legacy compiler is deprecated. Migrate your project to the new IR-based compiler", level = DeprecationLevel.HIDDEN)
    val BOTH: KotlinJsCompilerType
        get() = KotlinJsCompilerType.BOTH
}