/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.internal.properties.propertiesService
import java.io.File

// Property used for tests.
internal const val FUS_STATISTICS_PATH = "kotlin.session.logger.root.path"

internal fun Project.getFusDirectoryFromPropertyService() = getFusRootDirectoryFromPropertyService().resolve("kotlin-profile")
private fun Project.getFusRootDirectoryFromPropertyService() = propertiesService.get().get(FUS_STATISTICS_PATH, project)
    ?.also {
        logger.warn("$FUS_STATISTICS_PATH property for test purpose only")
    }?.let { File(it) } ?: project.gradle.gradleUserHomeDir


internal val Project.isCustomLoggerRootPathIsProvided
    get() = PropertiesBuildService.registerIfAbsent(this).get().get(FUS_STATISTICS_PATH, this) != null
