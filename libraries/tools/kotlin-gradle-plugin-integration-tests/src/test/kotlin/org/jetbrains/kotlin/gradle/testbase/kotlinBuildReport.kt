/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.toPath

private val reportLocationRegex = Regex("Kotlin build report is written to (file:\\S+)")
private val compileIterationRegex = Regex("""^ {2}Compile iteration:\R((?: {4}.*\R)*)""", RegexOption.MULTILINE)

/**
 * Reads the Kotlin build reports written by this build, joined into a single text.
 *
 * The report contains Kotlin's debug-level compilation logs.
 * This helps avoid running Gradle with debug logs (which is slower).
 *
 * Note: the build must run with [BuildOptions.buildReport] containing `BuildReportType.FILE`.
 */
fun BuildResult.kotlinBuildReport(): String =
    outputReader.useLines(::extractKotlinBuildReportLocations)
        .ifEmpty { error("Build output doesn't contain the location of a Kotlin build report") }
        .joinToString("\n") { it.toPath().readText() }

internal fun extractKotlinBuildReportLocations(outputLines: Sequence<String>): List<URI> =
    outputLines.mapNotNull { reportLocationRegex.find(it) }.map { URI(it.groupValues[1]) }.toList()

/**
 * Extracts the compilation log of the task under [taskPath] from a Kotlin build [report].
 */
fun extractKotlinBuildReportTaskLog(report: String, taskPath: String): String =
    Regex("""^Compilation log for task '${Regex.escape(taskPath)}':\R((?: {2}.*\R)*)""", RegexOption.MULTILINE)
        .find(report)?.groupValues?.get(1).orEmpty()

/**
 * Extracts the `.kt` files compiled by each compile iteration from a Kotlin build [report] or task log.
 *
 * The returned paths are relative to the project directory.
 *
 * Note: compiled files are only reported for incremental compile iterations.
 */
internal fun extractKotlinBuildReportCompileIterations(report: String): List<List<Path>> =
    compileIterationRegex.findAll(report)
        .map { match -> match.groupValues[1].lines().filter(String::isNotBlank).map { it.trim().substringBefore(" <- ") }.toPaths() }
        .toList()
