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
    taskName: String
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
