/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.launchInStage

/**
 * Returns true as soon as Gradle plugin with [pluginId] is applied.
 * Returns false if plugin wasn't applied during
 */
internal suspend fun Project.isPluginApplied(pluginId: String): Boolean {
    val result = CompletableFuture<Boolean>()
    pluginManager.withPlugin(pluginId) {
        check(!result.isCompleted) {
            "Plugin '$pluginId' was applied too late. It was expected to be applied during build script evaluation"
        }
        result.complete(true)
    }
    // All plugins must be applied during evaluation of build script
    launchInStage(AfterEvaluateBuildscript) {
        if (!result.isCompleted) result.complete(false)
    }

    return result.await()
}