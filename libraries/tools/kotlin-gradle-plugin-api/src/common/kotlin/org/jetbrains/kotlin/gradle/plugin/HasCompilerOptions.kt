/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

/**
 * @suppress
 */
@Deprecated(
    message = "Not consistent with other DSL to configure compiler options."
)
interface HasCompilerOptions<out CO : KotlinCommonCompilerOptions> {
    /**
     * @suppress
     */
    val options: CO

    /**
     * @suppress
     */
    fun configure(configuration: CO.() -> Unit) {
        configuration(options)
    }

    /**
     * @suppress
     */
    fun configure(configuration: Action<@UnsafeVariance CO>) {
        configuration.execute(options)
    }
}
