/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.jetbrains.kotlin.gradle.KotlinTopLevelDependencies

/**
 * Dependency container for different scopes that Kotlin projects can have.
 *
 * @since 2.2.20
 *
 */
@KotlinTopLevelDependencies
interface KotlinDependencies : Dependencies {
    val implementation: DependencyCollector
    val api: DependencyCollector
    val compileOnly: DependencyCollector
    val runtimeOnly: DependencyCollector
}

/**
 * Represents Kotlin Top-Level dependencies block
 *
 * @since 2.2.20
 */
@KotlinTopLevelDependencies
interface KotlinLevelDependenciesDsl : KotlinDependencies {
    fun test(code: Action<KotlinDependencies>) = test(code::execute)

    fun test(code: KotlinDependencies.() -> Unit)
}
