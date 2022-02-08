/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test.fixes.android

import org.gradle.api.Project

internal class TestFixesProperties(
    private val project: Project
) {
    val androidDebugKeystoreLocation: String
        get() = project.findProperty(ANDROID_DEBUG_KEYSTORE_LOCATION) as String? ?: throw IllegalArgumentException(
            "$ANDROID_DEBUG_KEYSTORE_LOCATION property was not found in 'gradle.properties'."
        )

    companion object {
        private const val PROP_PREFIX = "test.fixes."
        private const val ANDROID_DEBUG_KEYSTORE_LOCATION = "${PROP_PREFIX}android.debugKeystore"
    }
}
