/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

/**
 * Represents a Kotlin DSL entity holding information about possible modes for Kotlin/JS compiler.
 */
interface KotlinJsCompilerTypeHolder {

    /**
     * @suppress
     */
    @Deprecated("Because only IR compiler is left, no more necessary to know about compiler type in properties")
    val compilerTypeFromProperties: KotlinJsCompilerType?

    /**
     * Default type of the Kotlin/JS compiler to be used.
     */
    val defaultJsCompilerType: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    /**
     * @suppress
     */
    // Necessary to get rid of KotlinJsCompilerType import in build script
    @Deprecated("Legacy compiler is deprecated. Migrate your project to the new IR-based compiler", level = DeprecationLevel.HIDDEN)
    val LEGACY: KotlinJsCompilerType
        get() = KotlinJsCompilerType.LEGACY

    /**
     * Represents the IR (Intermediate Representation) backend mode of the Kotlin compiler.
     *
     * @see KotlinJsCompilerType.IR
     */
    val IR: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    /**
     * @suppress
     */
    @Deprecated("Legacy compiler is deprecated. Migrate your project to the new IR-based compiler", level = DeprecationLevel.HIDDEN)
    val BOTH: KotlinJsCompilerType
        get() = KotlinJsCompilerType.BOTH
}