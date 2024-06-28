/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project

internal fun Project.whenKaptEnabled(action: () -> Unit) = whenPluginsEnabled(
    setOf("org.jetbrains.kotlin.kapt", "kotlin-kapt"),
    action,
)

internal fun Project.whenMppEnabled(action: () -> Unit) = whenPluginsEnabled(
    setOf("org.jetbrains.kotlin.multiplatform", "kotlin-multiplatform"),
    action,
)

internal fun Project.whenJsOrMppEnabled(action: () -> Unit) = whenPluginsEnabled(
    setOf("org.jetbrains.kotlin.js", "org.jetbrains.kotlin.multiplatform", "kotlin-multiplatform"),
    action,
)

private fun Project.whenPluginsEnabled(
    pluginIds: Set<String>,
    action: () -> Unit,
) {
    var triggered = false

    fun trigger() {
        if (triggered) return
        triggered = true
        action()
    }

    for (pluginId in pluginIds) {
        pluginManager.withPlugin(pluginId) { trigger() }
    }
}