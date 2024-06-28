/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import java.io.File

internal class GradleCompilerEnvironment(
    val compilerClasspath: Iterable<File>,
    messageCollector: GradleErrorMessageCollector,
    outputItemsCollector: OutputItemsCollector,
    val outputFiles: List<File>,
    val reportingSettings: ReportingSettings,
    val compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel,
    val incrementalCompilationEnvironment: IncrementalCompilationEnvironment? = null,
    val kotlinScriptExtensions: Array<String> = emptyArray(),
) : CompilerEnvironment(Services.EMPTY, messageCollector, outputItemsCollector) {

    fun compilerFullClasspath(
        toolsJar: File?
    ): List<File> = if (toolsJar != null) compilerClasspath + toolsJar else compilerClasspath.toList()
}

