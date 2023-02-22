/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

/**
 * Analogous to [KotlinCompile] for K2
 * This does not extend [KotlinCompile], since [KotlinCompile] carries an unwanted/conflicting
 * type parameter `<out T : KotlinCommonOptions>`
 */
internal interface K2Compile : Task {
    @get:Internal
    val kotlinOptions: KotlinCommonOptions

    @get:Nested
    val multiplatformStructure: K2MultiplatformStructure
}
