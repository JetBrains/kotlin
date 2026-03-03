/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

/**
 * Represents a Kotlin DSL entity holding information about possible modes for the Kotlin/JS compiler.
 */
interface KotlinJsCompilerTypeHolder {

    /**
     * The default mode of the Kotlin/JS compiler to be used.
     */
    val defaultJsCompilerType: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    /**
     * Represents the Kotlin/JS IR (intermediate representation) compiler's backend mode.
     *
     * @see KotlinJsCompilerType.IR
     */
    val IR: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR
}
