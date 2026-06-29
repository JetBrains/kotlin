/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

/**
 * Different modes that can be used to configure the level of unused return value checking for the
 * [KotlinBaseExtension.returnValueCheckerMode] option.
 */
enum class ReturnValueCheckerMode {
    /**
     * Disables checking of unused return values.
     */
    Disabled,

    /**
     * Checks for unused return values of declarations that are explicitly marked as must-use.
     */
    Check,

    /**
     * Treats all declarations as must-use and checks for their unused return values.
     */
    Full;
}
