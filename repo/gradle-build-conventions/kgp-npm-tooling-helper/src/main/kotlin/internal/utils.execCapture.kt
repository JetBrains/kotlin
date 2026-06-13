/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.kgpnpmtooling.internal

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Runs [ExecOperations.exec] and captures stdout and stderr.
 *
 * On failure (non-zero exit code), the logs are appended to the exception message.
 */
internal fun ExecOperations.execCapture(
    workDir: File,
    commandLine: List<String>,
): ExecResult {
    val stdOut = ByteArrayOutputStream()
    val errorOut = ByteArrayOutputStream()

    val result =
        exec { exec ->
            exec.commandLine(commandLine)
            exec.workingDir(workDir)
            exec.standardOutput = stdOut
            exec.errorOutput = errorOut
            exec.isIgnoreExitValue = true
        }

    require(result.exitValue == 0) {
        buildString {
            appendLine("exec $commandLine failed: ${result.exitValue}")
            appendLine("stdOut:")
            appendLine(stdOut.toString())
            appendLine("errorOut:")
            appendLine(errorOut.toString())
        }
    }

    return ExecResult(
        exitValue = result.exitValue,
        stdOut = stdOut.toString(),
        errorOut = errorOut.toString(),
    )
}

internal data class ExecResult(
    val exitValue: Int,
    val stdOut: String,
    val errorOut: String,
)
