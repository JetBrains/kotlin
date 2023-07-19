/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions

/**
 * Represents a Kotlin task performing further processing of compiled code via additional Kotlin tools using configurable [toolOptions].
 *
 * Check [KotlinCommonCompilerToolOptions] inheritors (excluding [KotlinCommonCompilerToolOptions]) for the possible configuration
 * options.
 *
 * @see [KotlinCommonCompilerToolOptions]
 */
interface KotlinToolTask<out TO : KotlinCommonCompilerToolOptions> : Task {

    /**
     * Represents the tool options used by a Kotlin task with reasonable configured defaults.
     *
     * Could be used to either get the values of currently configured options or to modify them.
     */
    @get:Nested
    val toolOptions: TO

    /**
     * Configures the [toolOptions] with the provided configuration.
     */
    fun toolOptions(configure: TO.() -> Unit) {
        configure(toolOptions)
    }

    /**
     * Configures the [toolOptions] with the provided configuration.
     */
    fun toolOptions(configure: Action<in TO>) {
        configure.execute(toolOptions)
    }
}