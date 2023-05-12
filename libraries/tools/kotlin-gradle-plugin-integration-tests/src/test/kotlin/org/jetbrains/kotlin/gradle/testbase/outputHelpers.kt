/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.intellij.lang.annotations.Language

@Language("RegExp")
private fun taskOutputRegex(
    taskName: String,
) = """
    \[org\.gradle\.internal\.operations\.DefaultBuildOperationRunner] Build operation 'Task $taskName' started
    ([\s\S]+?)
    \[org\.gradle\.internal\.operations\.DefaultBuildOperationRunner] Build operation 'Task $taskName' completed
    """.trimIndent()
    .replace("\n", "")
    .toRegex()

/**
 * Filter [BuildResult.getOutput] for specific task with given [taskPath]
 *
 * Requires using [LogLevel.DEBUG].
 */
fun BuildResult.getOutputForTask(taskPath: String): String = getOutputForTask(taskPath, output)

/**
 * Filter give output for specific task with given [taskPath]
 *
 * Requires using [LogLevel.DEBUG].
 */
fun getOutputForTask(taskPath: String, output: String): String = taskOutputRegex(taskPath)
    .find(output)
    ?.let { it.groupValues[1] }
    ?: error("Could not find output for task $taskPath")

class CommandLineArguments(
    val args: List<String>,
    val buildResult: BuildResult,
)

/**
 * Asserts the command line arguments of the given Kotlin/Native (K/N) compiler for the specified tasks' paths.
 *
 * Note: The log level of the output must be set to [LogLevel.DEBUG].
 *
 * @param tasksPaths The paths of the tasks for which the command line arguments should be checked against the provided assertions.
 * @param toolName The name of the build tool used.
 * @param assertions The assertions to be applied to each command line argument of each given task.
 *                   These assertions validate the expected properties of the command line arguments.
 */
fun BuildResult.extractNativeTasksCommandLineArgumentsFromOutput(
    vararg tasksPaths: String,
    toolName: NativeToolKind = NativeToolKind.KONANC,
    assertions: CommandLineArguments.() -> Unit,
) = tasksPaths.forEach { taskPath ->
    val taskOutput = getOutputForTask(taskPath)
    val commandLineArguments = extractNativeCompilerCommandLineArguments(taskOutput, toolName)
    assertions(
        CommandLineArguments(commandLineArguments, this)
    )
}
