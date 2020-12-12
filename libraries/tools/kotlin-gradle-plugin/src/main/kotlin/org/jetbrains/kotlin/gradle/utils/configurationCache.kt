/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Task
import org.gradle.api.invocation.Gradle

internal fun isConfigurationCacheAvailable(gradle: Gradle) =
    try {
        val startParameters = gradle.startParameter
        startParameters.javaClass.getMethod("isConfigurationCache").invoke(startParameters) as? Boolean
    } catch (_: Exception) {
        null
    } ?: false

internal fun Task.disableTaskOnConfigurationCacheBuild(transientFieldAccessor: () -> Unit) {
    if (isConfigurationCacheAvailable(project.gradle)) {
        onlyIf {
            logger.warn("Configuration cache is not yet fully supported: use it at your own risk.")
            try {
                // transientFieldAccessor() will throw an exception after loading task from configuration cache
                transientFieldAccessor()
                true
            } catch (e: Exception) {
                logger.warn("Task cannot be executed because of corrupted state after loading from configuration cache.")
                false
            }
        }
    }
}