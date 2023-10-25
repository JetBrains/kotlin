/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.launchInStage

/**
 * Executes the given block of code if the Gradle Plugin with [pluginId] is applied to the project. Otherwise,
 * executes another block of code.
 */
internal fun <T> Project.withPluginId(
    pluginId: String,
    whenApplied: () -> T,
    whenNotApplied: () -> T,
): Future<T> {
    val result = CompletableFuture<T>()
    pluginManager.withPlugin(pluginId) { result.complete(whenApplied()) }
    // All plugins must be applied during evaluation of build script
    launchInStage(AfterEvaluateBuildscript) { if (!result.isCompleted) result.complete(whenNotApplied()) }

    return result
}