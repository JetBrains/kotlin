/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.tasks.findToolsJar
import java.io.File

internal class GradleCompilerEnvironment(
    val compilerClasspath: List<File>,
    messageCollector: GradlePrintingMessageCollector,
    outputItemsCollector: OutputItemsCollector,
    val outputFiles: FileCollection,
    val reportingSettings: ReportingSettings,
    val incrementalCompilationEnvironment: IncrementalCompilationEnvironment? = null,
    val kotlinScriptExtensions: Array<String> = emptyArray()
) : CompilerEnvironment(Services.EMPTY, messageCollector, outputItemsCollector) {
    val toolsJar: File? by lazy { findToolsJar() }

    val compilerFullClasspath: List<File>
        get() = (compilerClasspath + toolsJar).filterNotNull()
}

