/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.relativeTo

private val kotlinSrcRegex by lazy { Regex("\\[KOTLIN] compile iteration: ([^\\r\\n]*)") }

private val javaSrcRegex by lazy { Regex("\\[DEBUG] \\[[^]]*JavaCompiler] Compiler arguments: ([^\\r\\n]*)") }

/**
 * Extracts the list of compiled .kt files from the build output.
 *
 * The returned paths are relative to the project directory.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun extractCompiledKotlinFiles(projectDirectory: Path, output: String): List<Path> {
    return kotlinSrcRegex.findAll(output).asIterable()
        .flatMap { matchResult ->
            matchResult.groups[1]!!.value.split(", ")
                .map { source -> projectDirectory.resolve(source).normalize() }
        }
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
fun GradleProject.assertCompiledKotlinSources(expectedSources: Iterable<Path>, output: String) {
    val actualSources = extractCompiledKotlinFiles(projectPath, output).map {
        it.relativeTo(projectPath)
    }
    assertSameFiles(expectedSources, actualSources, "Compiled Kotlin files differ:\n")
}

/**
 * Asserts that compilation was non-incremental.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
@Suppress("unused")
fun BuildResult.assertNonIncrementalCompilation() {
    assertOutputContains("Non-incremental compilation will be performed")
}

/**
 * Asserts that compilation was incremental and the set of compiled .kt files exactly match [expectedCompiledKotlinFiles].
 *
 * [expectedCompiledKotlinFiles] are relative to the project directory.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 */
fun GradleProject.assertIncrementalCompilation(
    buildResult: BuildResult,
    expectedCompiledKotlinFiles: Iterable<String>
) {
    buildResult.assertOutputDoesNotContain("Non-incremental compilation will be performed")

    val actualCompiledKotlinFiles = extractCompiledKotlinFiles(
        projectPath,
        buildResult.output
    ).map {
        projectPath.relativize(it)
    }
    assertSameFiles(expectedCompiledKotlinFiles.toPaths(), actualCompiledKotlinFiles, "Compiled Kotlin files differ:\n")
}
