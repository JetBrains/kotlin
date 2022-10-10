/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions

interface KotlinToolTask<out TO : KotlinCommonCompilerToolOptions> : Task {
    @get:Nested
    val toolOptions: TO

    fun toolOptions(configure: TO.() -> Unit) {
        configure(toolOptions)
    }

    fun toolOptions(configure: Action<in TO>) {
        configure.execute(toolOptions)
    }
}