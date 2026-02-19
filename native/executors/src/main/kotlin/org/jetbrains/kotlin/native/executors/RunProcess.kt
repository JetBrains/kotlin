/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.time.Duration

data class RunProcessResult(
    /**
     * Execution time of the process. Can be a bit bigger than [ExecuteRequest.timeout].
     */
    val executionTime: Duration,
    /**
     * Captured standard output of the process.
     */
    val stdout: String,
    /**
     * Captured standard error of the process.
     */
    val stderr: String,
) {
    /**
     * Full output of the process: [stdout] followed by [stderr].
     */
    val output: String
        get() = stdout + stderr
}

class RunProcessException(
    message: String,
    private val result: RunProcessResult,
    /**
     * Process exit code if it exited by itself, or `null` if it was killed by a timeout.
     */
    val exitCode: Int?,
) : IllegalStateException(
    """
    |$message
    |stdout: ${result.stdout}
    |stderr: ${result.stderr}
    """.trimMargin()
) {
    /**
     * Execution time of the process. Can be a bit bigger than [ExecuteRequest.timeout].
     */
    val executionTime by result::executionTime

    /**
     * Captured standard output of the process.
     */
    val stdout by result::stdout

    /**
     * Captured standard error of the process.
     */
    val stderr by result::stderr

    /**
     * Full output of the process: [stdout] followed by [stderr].
     */
    val output by result::output
}

/**
 * Run [executableAbsolutePath] with [Executor] using current process' working directory with [args] and capture the full output.
 *
 * A simplified version of [Executor.execute].
 *
 * @param executableAbsolutePath Path to the executable
 * @param args Command line args
 * @param block optional block to additionally customize [ExecuteRequest]
 *
 * @throws RunProcessException if the process has failed or timed out.
 */
inline fun Executor.runProcess(
    executableAbsolutePath: String,
    vararg args: String,
    block: ExecuteRequest.() -> Unit = {},
): RunProcessResult {
    ByteArrayOutputStream().use { stdout ->
        ByteArrayOutputStream().use { stderr ->
            val request = ExecuteRequest(executableAbsolutePath).apply {
                this.args.addAll(args)
                this.stdout = stdout
                this.stderr = stderr
                this.workingDirectory = File("").absoluteFile
                this.block()
            }
            val response = this.execute(request)
            val result = RunProcessResult(
                executionTime = response.executionTime,
                stdout = stdout.toString("UTF-8").trim(),
                stderr = stderr.toString("UTF-8").trim(),
            )
            try {
                response.assertSuccess()
            } catch (e: IllegalStateException) {
                throw RunProcessException(e.message!!, result, response.exitCode)
            }
            return result
        }
    }
}

/**
 * Run [executableAbsolutePath] on host using current process' working directory with [args] and capture the full output.
 *
 * A simplified version of [HostExecutor.execute].
 *
 * @param executableAbsolutePath Path to the executable
 * @param args Command line args
 * @param block optional block to additionally customize [ExecuteRequest]
 *
 * @throws RunProcessException if the process has failed or timed out.
 */
inline fun runProcess(executableAbsolutePath: String, vararg args: String, block: ExecuteRequest.() -> Unit = {}) =
    HostExecutor().runProcess(executableAbsolutePath, *args, block = block)