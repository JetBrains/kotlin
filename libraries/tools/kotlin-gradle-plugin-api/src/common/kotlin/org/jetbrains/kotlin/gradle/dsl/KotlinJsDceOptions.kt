/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

/**
 * Options for the Kotlin JavaScript dead code elimination tool.
 */
@OptIn(InternalKotlinGradlePluginApi::class)
@Deprecated(KOTLIN_JS_DCE_TOOL_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
interface KotlinJsDceOptions : @Suppress("DEPRECATION") KotlinCommonToolOptions {
    /**
     * @suppress
     */
    override val options: @Suppress("DEPRECATION_ERROR") KotlinJsDceCompilerToolOptions

    /**
     * Development mode: don't strip out any code, just copy dependencies.
     *
     * Default value: false
     */
    @Suppress("unused")
    var devMode: Boolean
        get() = options.devMode.get()
        set(value) = options.devMode.set(value)
}

/**
 * @suppress
 */
@InternalKotlinGradlePluginApi
const val KOTLIN_JS_DCE_TOOL_DEPRECATION_MESSAGE: String =
    "The DCE tool is obsolete and does not work with the IR compiler. " +
            "The IR compiler supports dead code elimination of ouf the box. " +
            "It is enabled by default when compiling for production and disabled when compiling for development. " +
            "DCE roots can be specified by annotating root declarations with the '@JsExport' annotation"
