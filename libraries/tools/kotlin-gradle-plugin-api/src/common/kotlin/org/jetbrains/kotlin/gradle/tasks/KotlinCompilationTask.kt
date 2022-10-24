/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

interface KotlinCompilationTask<out CO : KotlinCommonCompilerOptions> : Task {
    @get:Nested
    val compilerOptions: CO

    fun compilerOptions(configure: CO.() -> Unit) {
        configure(compilerOptions)
    }

    fun compilerOptions(configure: Action<in CO>) {
        configure.execute(compilerOptions)
    }
}
