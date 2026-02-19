/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

/**
 * Represents a Kotlin DSL entity holding information about possible modes for the Kotlin/JS compiler.
 */
interface KotlinJsCompilerTypeHolder {

    /**
     * @suppress
     */
    @Deprecated(
        "Because only the IR compiler is left, it's no longer necessary to know about the compiler type in properties. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    val compilerTypeFromProperties: KotlinJsCompilerType?

    /**
     * The default mode of the Kotlin/JS compiler to be used.
     */
    val defaultJsCompilerType: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    /**
     * @suppress
     */
    // Necessary to get rid of KotlinJsCompilerType import in build script
    @Deprecated("The legacy compiler is deprecated. Migrate your project to the new IR-based compiler", level = DeprecationLevel.HIDDEN)
    val LEGACY: KotlinJsCompilerType
        @Suppress("DEPRECATION_ERROR")
        get() = KotlinJsCompilerType.LEGACY

    /**
     * Represents the Kotlin/JS IR (intermediate representation) compiler's backend mode.
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
        @Suppress("DEPRECATION_ERROR")
        get() = KotlinJsCompilerType.BOTH
}