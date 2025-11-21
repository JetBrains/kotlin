/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.gradle.plugin.mpp.baseModuleName
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.slf4j.Logger
import java.io.File

internal fun Project.klibModuleName(
    baseName: Provider<String> = baseModuleName(),
): Provider<String> = baseName.map {
    klibModuleName(it)
}

internal fun Project.klibModuleName(baseName: String = project.name): String =
    if (group.toString().isNotEmpty()) "$group:$baseName" else baseName

/**
 * Loads a single [KotlinLibrary] from the given [location].
 * In case of failure, returns `null` and reports the problem to [Project.logger].
 * The problem is reported at [LogLevel.INFO] level if [reportProblemsAtInfoLevel] is `true`, otherwise at [LogLevel.ERROR].
 */
internal fun Project.loadSingleKlib(location: File, reportProblemsAtInfoLevel: Boolean = false): KotlinLibrary? =
    loadSingleKlib(location, logger, reportProblemsAtInfoLevel)

/**
 * Loads a single [KotlinLibrary] from the given [location].
 * In case of failure, returns `null` and reports the problem to [logger].
 * The problem is reported at [LogLevel.INFO] level if [reportProblemsAtInfoLevel] is `true`, otherwise at [LogLevel.ERROR].
 */
internal fun loadSingleKlib(location: File, logger: Logger, reportProblemsAtInfoLevel: Boolean = false): KotlinLibrary? {
    val result = KlibLoader { libraryPaths(location) }.load()
    result.reportLoadingProblemsIfAny { _, message -> if (reportProblemsAtInfoLevel) logger.info(message) else logger.error(message) }
    return result.librariesStdlibFirst.singleOrNull()
}

