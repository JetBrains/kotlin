/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils.processes

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor


internal interface StreamsHandler : Closeable {
    /**
     * Collects whatever state is required the given process. Should not start work.
     */
    fun connectStreams(process: Process, processName: String, executor: Executor)

    /**
     * Starts reading/writing/whatever the process' streams. May block until the streams reach some particular state, e.g. indicate that the process has started successfully.
     */
    fun start()

    /** Disconnects from the process without waiting for further work. */
    fun disconnect()

    /**
     * Stops doing work with the process's streams. Should block until no further asynchronous work is happening on the streams.
     */
    override fun close()


    /**
     * A handler that writes nothing to the stdin of the [Process].
     */
    class EmptyStdIn : StreamsHandler {
        override fun connectStreams(process: Process, processName: String, executor: Executor) {
            process.outputStream.close()
        }

        override fun start() {}

        override fun close() {}

        override fun disconnect() {}
    }


    /**
     * Reads from the process' stdout and stderr (if not merged into stdout) and forwards to [OutputStream].
     */
    class OutputStreamsForwarder(
        private val standardOutput: OutputStream,
        private val errorOutput: OutputStream,
        private val readErrorStream: Boolean,
    ) : StreamsHandler {
        private val completed = CountDownLatch(if (readErrorStream) 2 else 1)
        private var executor: Executor? = null

        @Volatile
        private var standardOutputReader: ExecOutputHandleRunner? = null

        @Volatile
        private var standardErrorReader: ExecOutputHandleRunner? = null

        override fun connectStreams(process: Process, processName: String, executor: Executor) {
            this.executor = executor
            standardOutputReader =
                ExecOutputHandleRunner(
                    displayName = "read standard output of $processName",
                    inputStream = process.inputStream,
                    outputStream = standardOutput,
                    completed = completed,
                )
            if (readErrorStream) {
                standardErrorReader =
                    ExecOutputHandleRunner(
                        displayName = "read error output of $processName",
                        inputStream = process.errorStream,
                        outputStream = errorOutput,
                        completed = completed,
                    )
            }
        }

        override fun start() {
            if (readErrorStream) {
                executor!!.execute(standardErrorReader!!)
            }
            executor!!.execute(standardOutputReader!!)
        }

        override fun close() {
            completed.await()
        }

        override fun disconnect() {
            standardOutputReader!!.disconnect()
            if (readErrorStream) {
                standardErrorReader!!.disconnect()
            }
        }
    }


    /**
     * Forwards the contents of an [InputStream] to the process' stdin
     */
    class ForwardStdin(private val input: InputStream) : StreamsHandler {
        private val completed = CountDownLatch(1)
        private var executor: Executor? = null
        private var standardInputWriter: ExecOutputHandleRunner? = null

        override fun connectStreams(process: Process, processName: String, executor: Executor) {
            this.executor = executor

            standardInputWriter = ExecOutputHandleRunner(
                displayName = "write standard input to $processName",
                inputStream = input,
                outputStream = process.outputStream,
                completed = completed,
            )
        }

        override fun start() {
            executor!!.execute(standardInputWriter!!)
        }

        override fun close() {
            disconnect()
            completed.await()
        }

        override fun disconnect() {
            standardInputWriter!!.closeInput()
        }
    }
}

private class ExecOutputHandleRunner(
    private val displayName: String,
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val completed: CountDownLatch,
    private val bufferSize: Int = 8192, // Use the same default as java.io.BufferedReader
) : Runnable {

    @Volatile
    private var closed = false

    override fun run() {
        try {
            forwardContent()
        } finally {
            completed.countDown()
        }
    }

    private fun forwardContent() {
        try {
            val buffer = ByteArray(bufferSize)
            inputStream.use { input ->
                outputStream.use { output ->
                    while (!closed) {
                        val byteCount = input.read(buffer)
                        if (byteCount < 0) {
                            break
                        }
                        output.write(buffer, 0, byteCount)
                        output.flush()
                    }
                }
            }
        } catch (t: Throwable) {
            if (!closed && !t.isInterruption()) {
                logger.error("Could not $displayName.", t)
            }
        }
    }

    fun closeInput() {
        disconnect()
        inputStream.close()
    }

    override fun toString(): String = displayName

    fun disconnect() {
        closed = true
    }

    companion object {
        private val logger: Logger = Logging.getLogger(ExecOutputHandleRunner::class.java)

        /**
         * Interruption can happen e.g. on IBM JDK when a remote process was terminated.
         * Instead of returning `-1` on the next [InputStream.read] call, it will interrupt the current read call.
         */
        private fun Throwable.isInterruption(): Boolean =
            this is IOException && message == "Interrupted system call"
    }
}
