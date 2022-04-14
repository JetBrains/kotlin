/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.StartParameterInternal
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

internal fun unavailableValueError(propertyName: String): Nothing =
    error("'$propertyName' should be available at configuration time but unavailable on configuration cache reuse")

fun Task.notCompatibleWithConfigurationCache(reason: String) {
    val reportConfigurationCacheWarnings = try {
        val startParameters = project.gradle.startParameter as? StartParameterInternal
        startParameters?.run { isConfigurationCache && !isConfigurationCacheQuiet } ?: false
    } catch (_: IncompatibleClassChangeError) { // for cases when gradle is way too old
        false
    }

    if (!isGradleVersionAtLeast(7, 4)) {
        if (reportConfigurationCacheWarnings) {
            logger.warn("Task $name is not compatible with configuration cache: $reason")
        }
        return
    }

    try {
        val taskClass = Task::class.java
        val method = taskClass.getMethod("notCompatibleWithConfigurationCache", String::class.java)

        method.invoke(this, reason)
    } catch (e: ReflectiveOperationException) {
        if (reportConfigurationCacheWarnings) {
            logger.warn("Reflection issue - task $name is not compatible with configuration cache: $reason", e)
        }
    }
}