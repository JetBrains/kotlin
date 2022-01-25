/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.relativeTo
import org.gradle.api.logging.LogLevel
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.intellij.lang.annotations.Language

private val kotlinSrcRegex by lazy { Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)") }

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
 * Extracts list of compiled .kt files from task output
 *
 * Note: log level of output should be set to [LogLevel.DEBUG]
 */
fun extractKotlinCompiledSources(projectDirectory: Path, output: String) =
    kotlinSrcRegex.findAll(output)
        .asIterable()
        .flatMap { result ->
            result.groups[1]!!.value.split(", ")
                .map { source -> projectDirectory.resolve(source).normalize() }
        }

private val javaSrcRegex by lazy { Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)") }

/**
 * Extracts list of compiled .java files from task output
 *
 * Note: log level of output should be set to [LogLevel.DEBUG]
 */
fun extractJavaCompiledSources(output: String): List<Path> =
    javaSrcRegex.findAll(output).asIterable().flatMap {
        it.groups[1]!!.value
            .split(" ")
            .filter { source -> source.endsWith(".java", ignoreCase = true) }
            .map { source -> Paths.get(source).normalize() }
    }

/**
 * Asserts all the .kt files from [expectedSources] and only they are compiled
 *
 * Note: log level of output should be set to [LogLevel.DEBUG]
 */
fun GradleProject.assertCompiledKotlinSources(
    expectedSources: Iterable<Path>,
    output: String,
    errorMessageSuffix: String = ""
) {
    val actualSources = extractKotlinCompiledSources(projectPath, output).map {
        it.relativeTo(projectPath)
    }
    assertSameFiles(expectedSources, actualSources, "Compiled Kotlin files differ${errorMessageSuffix}:\n")
}

var testBuildRunId = 0

fun TestProject.checkTaskCompileClasspath(
    taskPath: String,
    checkModulesInClasspath: List<String> = emptyList(),
    checkModulesNotInClasspath: List<String> = emptyList(),
    isNative: Boolean = false
) {
    val subproject = taskPath.substringBeforeLast(":").takeIf { it.isNotEmpty() && it != taskPath }
    val taskName = taskPath.removePrefix(subproject.orEmpty())
    val taskClass = if (isNative) "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile<*, *>" else "AbstractCompile"
    val expression = """(tasks.getByName("$taskName") as $taskClass).${if (isNative) "libraries" else "classpath"}.toList()"""
    checkPrintedItems(subproject, expression, checkModulesInClasspath, checkModulesNotInClasspath)
}

private fun TestProject.checkPrintedItems(
    subproject: String?,
    itemsExpression: String,
    checkAnyItemsContains: List<String>,
    checkNoItemContains: List<String>
) = with(this) {
    val printingTaskName = "printItems${testBuildRunId++}"
    gradleBuildScript(subproject).appendText(
        """
        ${'\n'}
        tasks.create("$printingTaskName") {
            doLast {
                println("###$printingTaskName" + $itemsExpression)
            }
        }
        """.trimIndent()
    )
    build("${subproject?.prependIndent(":").orEmpty()}:$printingTaskName") {
        val itemsLine = output.lines().single { "###$printingTaskName" in it }.substringAfter(printingTaskName)
        val items = itemsLine.removeSurrounding("[", "]").split(", ").toSet()
        checkAnyItemsContains.forEach { pattern -> assertTrue { items.any { pattern in it } } }
        checkNoItemContains.forEach { pattern -> assertFalse { items.any { pattern in it } } }
    }
}