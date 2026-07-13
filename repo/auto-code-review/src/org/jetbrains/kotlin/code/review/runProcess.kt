/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

suspend fun runProcess(
    directory: File,
    input: String?,
    command: List<String>,
): ExecutionResult = withContext(Dispatchers.IO) {
    val stdinFile = input?.let {
        createTempFile("stdin", ".txt")
    }

    try {
        stdinFile?.writeText(input)
        val stdoutFile = createTempFile("stdout", ".txt")
        try {
            val stderrFile = createTempFile("stderr", ".txt")
            try {
                runProcess(
                    directory = directory,
                    stdinFile = stdinFile?.toFile(),
                    stdoutFile = stdoutFile.toFile(),
                    stderrFile = stderrFile.toFile(),
                    command = command
                )
            } finally {
                stderrFile.deleteExisting()
            }
        } finally {
            stdoutFile.deleteExisting()
        }
    } finally {
        stdinFile?.deleteExisting()
    }
}

suspend fun runProcess(
    directory: File,
    stdinFile: File?,
    stdoutFile: File,
    stderrFile: File,
    command: List<String>,
): ExecutionResult = withContext(Dispatchers.IO) {
    /*
    Even in JDK 25, ProcessBuilder still doesn't properly support arguments with quotes inside on Windows.
    But some usages of this function use such arguments.
    For example, when running `claude` and passing `--settings` or `--json-schema`.
    To fix the issue, this code should be run with `-Djdk.lang.Process.allowAmbiguousCommands=false`.
    The build script takes care of that.
    */

    val processBuilder = ProcessBuilder(command)
        .directory(directory)
        .apply { if (stdinFile != null) redirectInput(stdinFile) }
        .redirectOutput(stdoutFile)
        .redirectError(stderrFile)

    val exitCode = processBuilder.start().onExit().await().exitValue()

    ExecutionResult(
        tool = command.first(),
        exitCode = exitCode,
        stdout = stdoutFile.readText().trim(),
        stderr = stderrFile.readText().trim()
    )
}

class ExecutionResult(
    val tool: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    fun checkExitCode() {
        check(exitCode == 0) { "$tool exit code: $exitCode\nstdout:$stdout\nstderr:\n$stderr" }
    }
}
