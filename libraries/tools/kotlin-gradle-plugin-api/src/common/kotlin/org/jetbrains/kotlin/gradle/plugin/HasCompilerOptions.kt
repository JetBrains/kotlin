/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.dsl.HasCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

@Deprecated("Use 'org.jetbrains.kotlin.gradle.dsl.HasCompilerOptions' instead")
interface HasCompilerOptions<out CO : KotlinCommonCompilerOptions> : HasCompilerOptions, KotlinCommonCompilerOptions {
    val options: CO
    override val compilerOptions: CO get() = options

    operator fun invoke(configuration: CO.() -> Unit) = configuration(options)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Replace with compilerOptions { }")
    fun configure(configuration: CO.() -> Unit) = invoke(configuration)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Replace with compilerOptions { }")
    fun configure(configuration: Action<@UnsafeVariance CO>) {
        configuration.execute(options)
    }
}
