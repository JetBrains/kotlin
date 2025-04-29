/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.intellij.lang.annotations.Language

@Language("RegExp")
private fun taskOutputRegexForDebugLog(
    taskName: String,
) = """
    \[org\.gradle\.internal\.operations\.DefaultBuildOperationRunner] Build operation 'Task $taskName' started
    ([\s\S]+?)
    \[org\.gradle\.internal\.operations\.DefaultBuildOperationRunner] Build operation 'Task $taskName' completed
    """.trimIndent()
    .replace("\n", "")
    .toRegex()

@Language("RegExp")
private fun taskOutputRegexForInfoLog(
    taskName: String,
) =
    """
    ^\s*$\r?
    ^> Task $taskName$\r?
    ([\s\S]*?\[KOTLIN][\s\S]*?|[\s\S]+?)\r?
    ^\s*$\r?
    """.trimIndent()
        .toRegex(RegexOption.MULTILINE)

/**
 * Gets the output produced by a specific task during a Gradle build.
 *
 * @param taskPath The path of the task whose output should be retrieved.
 * @param logLevel The given output contains no more than the [logLevel] logs.
 *
 * @return The output produced by the specified task during the build.
 *
 * @throws IllegalStateException if the specified task path does not match any tasks in the build.
 */
fun BuildResult.getOutputForTask(taskPath: String, logLevel: LogLevel = LogLevel.DEBUG): String =
    getOutputForTask(taskPath, output, logLevel)

/**
 * Gets the output produced by a specific task during a Gradle build.
 *
 * @param taskPath The path of the task whose output should be retrieved.
 * @param output The output from which we should extract task's output
 * @param logLevel The given output contains no more than the [logLevel] logs.
 *
 * @return The output produced by the specified task during the build.
 *
 * @throws IllegalStateException if the specified task path does not match any tasks in the build.
 */
fun getOutputForTask(taskPath: String, output: String, logLevel: LogLevel = LogLevel.DEBUG): String = (
        when (logLevel) {
            LogLevel.INFO -> taskOutputRegexForInfoLog(taskPath)
            LogLevel.DEBUG -> taskOutputRegexForDebugLog(taskPath)
            else -> throw IllegalStateException("Unsupported log lever for task output was given: $logLevel")
        })
    .findAll(output)
    .map { it.groupValues[1] }
    .joinToString(System.lineSeparator())
    .ifEmpty {
        error(
            """
            Could not find output for task $taskPath.
            =================
            Build output is:
            $output 
            =================     
            """.trimIndent()
        )
    }

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
 * @param logLevel The given output contains no more than the [logLevel] logs.
 * @param assertions The assertions to be applied to each command line argument of each given task.
 *                   These assertions validate the expected properties of the command line arguments.
 *                   These assertions validate the expected properties of the command line arguments.
 */
fun BuildResult.extractNativeTasksCommandLineArgumentsFromOutput(
    vararg tasksPaths: String,
    toolName: NativeToolKind = NativeToolKind.KONANC,
    logLevel: LogLevel = LogLevel.INFO,
    assertions: CommandLineArguments.() -> Unit,
) = tasksPaths.forEach { taskPath ->
    val taskOutput = getOutputForTask(taskPath, logLevel)
    val commandLineArguments = extractNativeCompilerCommandLineArguments(taskOutput, toolName)
    assertions(
        CommandLineArguments(commandLineArguments, this)
    )
}
