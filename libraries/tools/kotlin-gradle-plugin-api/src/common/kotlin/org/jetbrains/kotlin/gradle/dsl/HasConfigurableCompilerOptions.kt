/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * DSL entity with the ability to configure Kotlin compiler options.
 */
@ExperimentalKotlinGradlePluginApi
interface HasConfigurableCompilerOptions<CO : KotlinCommonCompilerOptions> {

    /**
     * Represents the compiler options used by a Kotlin compilation process.
     *
     * This can be used to get the values of currently configured options or modify them.
     */
    @ExperimentalKotlinGradlePluginApi
    val compilerOptions: CO

    /**
     * Configures the [compilerOptions] with the provided configuration.
     */
    @ExperimentalKotlinGradlePluginApi
    fun compilerOptions(configure: CO.() -> Unit) {
        configure(compilerOptions)
    }

    /**
     * Configures the [compilerOptions] with the provided configuration.
     */
    @ExperimentalKotlinGradlePluginApi
    fun compilerOptions(configure: Action<CO>) {
        configure.execute(compilerOptions)
    }
}
