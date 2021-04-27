/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

internal fun isConfigurationCacheAvailable(gradle: Gradle) =
    try {
        val startParameters = gradle.startParameter
        startParameters.javaClass.getMethod("isConfigurationCache").invoke(startParameters) as? Boolean
    } catch (_: Exception) {
        null
    } ?: false

internal fun Project.getSystemProperty(key: String): String? {
    return if (isConfigurationCacheAvailable(gradle)) {
        providers.systemProperty(key).forUseAtConfigurationTime().orNull
    } else {
        System.getProperty(key)
    }
}