/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val kotlinSrcRegex by lazy { Regex("\\[KOTLIN] compile iteration: ([^\\r\\n]*)") }

private val javaSrcRegex by lazy { Regex("\\[DEBUG] \\[[^]]*JavaCompiler] Compiler arguments: ([^\\r\\n]*)") }

@Language("RegExp")
private fun taskOutputRegex(
    taskName: String
) = """
(?:
\[LIFECYCLE] \[class org\.gradle(?:\.internal\.buildevents)?\.TaskExecutionLogger] :$taskName|
\[org\.gradle\.execution\.(?:plan|taskgraph)\.Default(?:Task)?PlanExecutor] :$taskName.*?started
)
([\s\S]+?)
(?:
Finished executing task ':$taskName'|
\[org\.gradle\.execution\.(?:plan|taskgraph)\.Default(?:Task)?PlanExecutor] :$taskName.*?completed
)
""".trimIndent()
    .replace("\n", "")
    .toRegex()

/**
 * Filter [BuildResult.getOutput] for specific task with given [taskName].
 *
 * Requires using [LogLevel.DEBUG].
 */
fun BuildResult.getOutputForTask(taskName: String): String = taskOutputRegex(taskName)
    .find(output)
    ?.let { it.groupValues[1] }
    ?: error("Could not find output for task $taskName")

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
fun BuildResult.assertIncrementalCompilation(expectedCompiledKotlinFiles: Iterable<String>) {
    assertOutputDoesNotContain("Non-incremental compilation will be performed")

    val actualCompiledKotlinFiles = extractCompiledKotlinFiles(output)
    assertSameFiles(expectedCompiledKotlinFiles.toPaths(), actualCompiledKotlinFiles, "Compiled Kotlin files differ:\n")
}
