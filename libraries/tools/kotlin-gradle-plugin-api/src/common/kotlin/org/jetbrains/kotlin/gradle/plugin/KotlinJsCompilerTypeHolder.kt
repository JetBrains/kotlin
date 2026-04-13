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
    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Remove compiler type selection from the DSL. Scheduled for removal in Kotlin 2.6.",
        level = DeprecationLevel.WARNING,
    )
    val defaultJsCompilerType: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR

    /**
     * Represents the Kotlin/JS IR (intermediate representation) compiler's backend mode.
     *
     * @see KotlinJsCompilerType.IR
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Remove compiler type selection from the DSL. Scheduled for removal in Kotlin 2.6.",
        level = DeprecationLevel.WARNING,
    )
    val IR: KotlinJsCompilerType
        get() = KotlinJsCompilerType.IR
}
