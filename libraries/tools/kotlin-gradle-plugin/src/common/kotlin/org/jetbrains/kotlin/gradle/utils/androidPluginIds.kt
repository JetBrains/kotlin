/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project

internal val androidPluginIds = listOf(
    "com.android.application",
    "com.android.library",
    "com.android.dynamic-feature",
    "com.android.test",
    // Deprecated android plugins
    "com.android.instantapp",
    "com.android.feature"
    // For following plugins 'kotlin-android' should never be applied
    // see https://issuetracker.google.com/issues/228449122
    //"com.android.asset-pack",
    //"com.android.asset-pack-bundle",
)

internal fun Project.findAppliedAndroidPluginIdOrNull(): String? {
    return androidPluginIds.firstOrNull { androidPluginId -> plugins.findPlugin(androidPluginId) != null }
}
