/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheEnabled

internal fun Project.readSystemPropertyAtConfigurationTime(key: String): Provider<String> {
    return providers.systemProperty(key)
}

fun Task.notCompatibleWithConfigurationCacheCompat(reason: String) {
    val reportConfigurationCacheWarnings = try {
        val requested = project.isConfigurationCacheEnabled
        val startParameters = project.gradle.startParameter as? StartParameterInternal
        requested && (startParameters?.isConfigurationCacheQuiet ?: false)
    } catch (_: IncompatibleClassChangeError) { // for cases when gradle is way too old
        false
    }

    if (GradleVersion.current() < GradleVersion.version("7.4")) {
        if (reportConfigurationCacheWarnings) {
            logger.warn("Task $name is not compatible with configuration cache: $reason")
        }
        return
    }

    notCompatibleWithConfigurationCache(reason)
}