/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project

internal fun Project.whenKaptEnabled(block: () -> Unit) {
    var triggered = false

    fun trigger() {
        if (triggered) return
        triggered = true
        block()
    }

    pluginManager.withPlugin("kotlin-kapt") { trigger() }
    pluginManager.withPlugin("org.jetbrains.kotlin.kapt") { trigger() }
}