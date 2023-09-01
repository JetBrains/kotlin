/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val kotlinSrcRegex by lazy { Regex("\\[KOTLIN] compile iteration: ([^\\r\\n]*)") }

private val javaSrcRegex by lazy { Regex("\\[DEBUG] \\[[^]]*JavaCompiler] Compiler arguments: ([^\\r\\n]*)") }

/**
 * Extracts the list of compiled .kt files from the build output.
 *
 * The returned paths are relative to the project directory.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun extractCompiledKotlinFiles(output: String): List<Path> {
    return kotlinSrcRegex.findAll(output).asIterable()
        .flatMap { matchResult -> matchResult.groups[1]!!.value.split(", ") }
        .toPaths()
}

/**
 * Extracts the list of compiled .java files from the build output.
 *
 * The returned paths are relative to the project directory.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun extractCompiledJavaFiles(projectDir: File, output: String): List<Path> {
    return javaSrcRegex.findAll(output).asIterable()
        .flatMap { matchResult -> matchResult.groups[1]!!.value.split(" ") }
        .filter { filePath -> filePath.endsWith(".java", ignoreCase = true) }
        .map { javaFilePath -> projectDir.toPath().relativize(Paths.get(javaFilePath)) }
}

/**
 * Asserts all the .kt files from [expectedSources] and only they are compiled
 *
 * Note: log level of output should be set to [LogLevel.DEBUG]
 */
fun assertCompiledKotlinSources(
    expectedSources: Iterable<Path>,
    output: String,
    errorMessageSuffix: String = ""
) {
    val actualSources = extractCompiledKotlinFiles(output)
    assertSameFiles(expectedSources, actualSources, "Compiled Kotlin files differ${errorMessageSuffix}:\n")
}

/**
 * Asserts all the .java files from [expectedSources] and only they are compiled
 *
 * Note: log level of output should be set to [LogLevel.DEBUG]
 */
fun GradleProject.assertCompiledJavaSources(
    expectedSources: Iterable<Path>,
    output: String,
    errorMessageSuffix: String = ""
) {
    val actualSources = extractCompiledJavaFiles(projectPath.toRealPath().toFile(), output)
    assertSameFiles(expectedSources, actualSources, "Compiled Java files differ${errorMessageSuffix}:\n")
}

/**
 * Asserts that compilation was non-incremental.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun BuildResult.assertNonIncrementalCompilation(reason: BuildAttribute? = null) {
    if (reason != null) {
        assertOutputContains("$NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED: ${reason.name}")
    } else {
        assertOutputContains(NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED)
    }

    // Also check that the other cases didn't happen
    assertOutputDoesNotContain(INCREMENTAL_COMPILATION_COMPLETED)
    assertOutputDoesNotContain(FALLING_BACK_TO_NON_INCREMENTAL_COMPILATION)
}

/**
 * Asserts that compilation was incremental and the set of compiled .kt files exactly match [expectedCompiledKotlinFiles].
 *
 * [expectedCompiledKotlinFiles] are relative to the project directory.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun BuildResult.assertIncrementalCompilation(expectedCompiledKotlinFiles: Iterable<Path>? = null) {
    assertOutputContains(INCREMENTAL_COMPILATION_COMPLETED)

    // Also check that the other cases didn't happen
    assertOutputDoesNotContain(NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED)
    assertOutputDoesNotContain(FALLING_BACK_TO_NON_INCREMENTAL_COMPILATION)

    expectedCompiledKotlinFiles?.let {
        assertSameFiles(expected = it, actual = extractCompiledKotlinFiles(output), "Compiled Kotlin files differ:\n")
    }
}

/**
 * Asserts that incremental compilation was attempted, failed, and fell back to non-incremental compilation.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun BuildResult.assertIncrementalCompilationFellBackToNonIncremental(reason: BuildAttribute? = null) {
    if (reason != null) {
        assertOutputContains("$FALLING_BACK_TO_NON_INCREMENTAL_COMPILATION (reason = ${reason.name})")
    } else {
        assertOutputContains(FALLING_BACK_TO_NON_INCREMENTAL_COMPILATION)
    }

    // Also check that the other cases didn't happen
    assertOutputDoesNotContain(INCREMENTAL_COMPILATION_COMPLETED)
    assertOutputDoesNotContain(NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED)
}

// Each of the following messages should uniquely correspond to a case in `IncrementalCompilerRunner.ICResult`
private const val INCREMENTAL_COMPILATION_COMPLETED = "Incremental compilation completed"
const val NON_INCREMENTAL_COMPILATION_WILL_BE_PERFORMED = "Non-incremental compilation will be performed"
private const val FALLING_BACK_TO_NON_INCREMENTAL_COMPILATION = "Falling back to non-incremental compilation"
