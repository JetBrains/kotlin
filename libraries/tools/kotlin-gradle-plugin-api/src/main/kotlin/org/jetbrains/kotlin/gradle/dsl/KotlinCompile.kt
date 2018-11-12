/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.Internal

interface KotlinCompile<out T : KotlinCommonOptions> : Task {
    @get:Internal
    val kotlinOptions: T

    fun kotlinOptions(fn: T.() -> Unit) {
        kotlinOptions.fn()
    }

    fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }
}