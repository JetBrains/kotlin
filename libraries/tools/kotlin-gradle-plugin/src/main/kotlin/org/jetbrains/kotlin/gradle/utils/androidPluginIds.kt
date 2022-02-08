/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

internal val androidPluginIds = listOf(
    "com.android.application",
    "com.android.library",
    "com.android.dynamic-feature",
    "com.android.asset-pack",
    "com.android.asset-pack-bundle",
    "com.android.lint",
    "com.android.test",
    // Deprecated android plugins
    "com.android.instantapp",
    "com.android.feature"
)
