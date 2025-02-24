/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

internal fun execWithProgress(
    logger: ProgressLogger,
    description: String,
    execOps: ExecOperations,
    configureExec: (execSpec: ExecSpec) -> Unit,
): ExecResult {
    return logger.operation(description) {
        this.progress(description)

        val stdout = StringBuilder()
        val stdoutInputPipe = PipedInputStream()
        val stdoutOutputPipe = PipedOutputStream(stdoutInputPipe)

        val outputReaderThread = createOutputReaderThread(
            description = description,
            stdout = stdout,
            stdoutInputPipe = stdoutInputPipe,
            logger = this,
        )

        val result = execOps.exec { exec ->
            exec.standardOutput = stdoutOutputPipe
            exec.isIgnoreExitValue = true
            configureExec(exec)
        }

        outputReaderThread.join()

        result.rethrowFailure()

        if (result.exitValue != 0) {
            error(
                """
                Process '$description' returns ${result.exitValue}
                $stdout
                """.trimIndent()
            )
        }
        result
    }
}

/**
 * Create a new thread for handing the process output.
 *
 * The original intention behind using a separate thread was not documented.
 * I am writing this doc retrospectively, making some guesses.
 *
 * Use a separate thread so the logger is output immediately, incrementally, while the process is running
 * to give better feedback to users.
 *
 * Strip out empty newlines to prevent the progress logger displaying empty lines.
 *
 * Remove backspace char `\b` to make sure the output is pretty.
 * Sometimes cli tools use the backspace char modify the console to do things like display progress bars.
 * We want to forward the logs to the Gradle console, so remove backspace chars to prevent disrupting the Gradle logs.
 *
 * All stdout will be appended to [stdout].
 */
private fun createOutputReaderThread(
    description: String,
    stdout: StringBuilder,
    stdoutInputPipe: PipedInputStream,
    logger: ProgressLogger,
): Thread =
    thread(
        name = "output reader for [$description]",
        isDaemon = true,
    ) {
        stdoutInputPipe.reader().use { reader ->
            val buffer = StringBuilder()
            while (true) {
                val read = reader.read()
                if (read == -1) break
                val ch = read.toChar()
                if (ch == '\b' || ch == '\n' || ch == '\r') {
                    if (buffer.isNotEmpty()) {
                        val str = buffer.toString()
                        stdout.append(str)
                        logger.progress(str.trim())
                        buffer.setLength(0)
                    }
                    stdout.append(ch)
                } else {
                    buffer.append(ch)
                }
            }
        }
    }


internal fun execWithErrorLogger(
    logger: ProgressLogger,
    description: String,
    execOps: ExecOperations,
    errorClient: TeamCityMessageCommonClient,
    standardClient: TeamCityMessageCommonClient,
    configureExec: (execSpec: ExecSpec) -> Unit,
): ExecResult {
    return logger.operation(description) {
        this.progress(description)

        val result = execOps.exec { exec ->
            exec.isIgnoreExitValue = true
            configureExec(exec)
        }
        if (result.exitValue != 0) {
            error(
                errorClient.testFailedMessage()
                    ?: standardClient.testFailedMessage()
                    ?: "Error occurred. See log for details."
            )
        }
        result
    }
}
