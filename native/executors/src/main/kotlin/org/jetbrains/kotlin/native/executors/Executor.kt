/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import java.io.*
import kotlin.time.Duration

private class CloseProtectedOutputStream(stream : OutputStream) : FilterOutputStream(stream) {
    override fun close() {
        // Make sure all the data is written out.
        super.flush()
        // Exchange out for a new dummy stream.
        this.out = ByteArrayOutputStream()
        // And now properly close the stream from above.
        super.close()
    }
}

data class ExecuteRequest(
        /**
         * Path to the executable.
         */
        var executableAbsolutePath: String,
        /**
         * Command line args.
         */
        val args: MutableList<String> = mutableListOf(),
        /**
         * Optional working directory. By default its the parent directory of [executableAbsolutePath].
         */
        var workingDirectory: File? = null,
        /**
         * Will be sent to the executable input and then closed. By default an empty stream.
         */
        var stdin: InputStream = ByteArrayInputStream(byteArrayOf()),
        /**
         * The output stream of the executable will be sent to this stream and then this stream will be closed. By default stdout of current process.
         */
        var stdout: OutputStream = CloseProtectedOutputStream(System.out),
        /**
         * The error stream of the executable will be sent to this stream and then this stream will be closed. By default stderr of current process.
         */
        var stderr: OutputStream = CloseProtectedOutputStream(System.err),
        /**
         * Additional environment variables.
         */
        val environment: MutableMap<String, String> = mutableMapOf(),
        /**
         * Bound execution time of the process. By default it's [Duration.INFINITE] meaning it's unbounded.
         */
        var timeout: Duration = Duration.INFINITE
) {
    /**
     * Create a copy of this [ExecuteRequest], modify the copy by running [block] on it, and return that copy.
     */
    inline fun copying(block: ExecuteRequest.() -> Unit): ExecuteRequest = copy().apply(block)
}

data class ExecuteResponse(
        /**
         * Process exit code if it exited by itself, or `null` if it was killed by a timeout.
         */
        val exitCode: Int?,
        /**
         * Execution time of the process. Can be a bit bigger than [ExecuteRequest.timeout].
         */
        val executionTime: Duration,
) {
    /**
     * Checks that [exitCode] is `0`.
     *
     * @throws IllegalStateException if [exitCode] is not 0.
     */
    fun assertSuccess(): ExecuteResponse {
        check(exitCode == 0) {
            if (exitCode == null) {
                "Timed out in $executionTime"
            } else {
                "Exited with code $exitCode in $executionTime"
            }
        }
        return this
    }
}

/**
 * Run a process in a specific execution environment.
 *
 * See subclasses for supported execution environments.
 *
 * NOTE: an [Executor] may cache some details about the environment (e.g. availability of an Xcode simulator).
 *       So, if the [Executor] lives in a long-running process (e.g. gradle daemon), it will miss environment changes. In that case,
 *       consider just recreating [Executor].
 */
interface Executor {
    /**
     * Run the process and wait for its completion.
     */
    fun execute(request: ExecuteRequest): ExecuteResponse
}
