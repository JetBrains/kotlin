/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)

package org.jetbrains.kotlin.native.executors

import kotlinx.coroutines.*
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class ProcessStreams(
    scope: CoroutineScope,
    process: Process,
    stdin: InputStream,
    stdout: OutputStream,
    stderr: OutputStream,
) {
    private val ignoreIOErrors = AtomicBoolean(false)
    private val stdin = scope.launch {
        stdin.apply {
            copyStreams(this, process.outputStream)
            close()
        }
        process.outputStream.close()
    }
    private val stdout = scope.launch {
        stdout.apply {
            copyStreams(process.inputStream, this)
            close()
        }
        process.inputStream.close()
    }
    private val stderr = scope.launch {
        stderr.apply {
            copyStreams(process.errorStream, this)
            close()
        }
        process.errorStream.close()
    }

    private fun copyStreams(from: InputStream, to: OutputStream) {
        try {
            from.copyTo(to)
        } catch (e: IOException) {
            if (ignoreIOErrors.get()) return
            throw e
        }
    }

    suspend fun drain() {
        // First finish passing input into the process.
        stdin.join()
        // Now receive all the output in whatever order.
        stdout.join()
        stderr.join()
    }

    fun cancel() {
        ignoreIOErrors.set(true)
        stdout.cancel()
        stderr.cancel()
        stdin.cancel()
    }
}

private object ProcessKiller {
    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            killAllProcesses()
        })
    }

    private var processes = ConcurrentLinkedQueue<Process>()

    private fun killAllProcesses() {
        processes.forEach {
            it.destroyForcibly()
        }
    }

    fun register(process: Process) = processes.add(process)

    fun deregister(process: Process) = processes.remove(process)
}

private fun <T> ProcessBuilder.scoped(block: suspend CoroutineScope.(Process) -> T): T {
    val process = start()
    // Make sure the process is killed even if the jvm process is being destroyed.
    // e.g. gradle --no-daemon task execution was cancelled by the user pressing ^C
    ProcessKiller.register(process)
    return try {
        val result = runBlocking(Dispatchers.IO) {
            block(process)
        }
        result
    } finally {
        // Make sure the process is killed even if the current thread was interrupted.
        // e.g. gradle task execution was cancelled by the user pressing ^C
        process.destroyForcibly()
        // The process is dead, no need to ensure its destruction during the shutdown.
        ProcessKiller.deregister(process)
    }
}

private class SleeperWithBackoff {
    // Start with exponential backoff, then try 150ms for a while, and finally settle on a second.
    private val backoffMilliseconds = longArrayOf(10, 20, 40, 80, 150, 150, 150, 150, 150)
    private val maxBackoffMilliseconds = 1000L
    private var nextIndex = 0

    private val nextBackoffMilliseconds: Long
        get() = backoffMilliseconds.getOrNull(nextIndex++) ?: maxBackoffMilliseconds

    fun sleep() {
        Thread.sleep(nextBackoffMilliseconds)
    }
}

private fun Process.waitFor(timeout: Duration): Boolean {
    if (!HostManager.hostIsMingw) return waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    // KT-65113: Looks like there's a race in waitFor implementation for Windows. It can wait for the entire `timeout` but the process'
    // exitValue would be 0.
    if (!isAlive) return true
    if (!timeout.isPositive()) return false
    val deadline = TimeSource.Monotonic.markNow() + timeout
    val sleeper = SleeperWithBackoff()
    do {
        sleeper.sleep()
        if (!isAlive) break
    } while (deadline.hasNotPassedNow())
    return !isAlive
}

/**
 * [Executor] that runs the process on the host system.
 */
class HostExecutor : Executor {
    private val logger = Logger.getLogger(HostExecutor::class.java.name)

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile
        val commandLine = "${request.executableAbsolutePath}${request.args.joinToString(separator = " ", prefix = " ")}"
        val environmentFormatted =
            request.environment.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\": \"${it.value}\"" }
        logger.info(
            """
                |Starting command: $commandLine
                |In working directory: ${workingDirectory.absolutePath}
                |With additional environment: $environmentFormatted
                |And timeout: ${request.timeout}
                """.trimMargin()
        )
        return ProcessBuilder(listOf(request.executableAbsolutePath) + request.args).apply {
            directory(workingDirectory)
            environment().putAll(request.environment)
        }.scoped { process ->
            val streams = ProcessStreams(this, process, request.stdin, request.stdout, request.stderr)
            val (isTimeout, duration) = measureTimedValue {
                !process.waitFor(request.timeout)
            }
            suspend fun cancel() {
                streams.cancel()
                process.destroyForcibly()
                streams.drain()
            }
            if (isTimeout) {
                logger.warning("Timeout running $commandLine in $duration")
                cancel()
                ExecuteResponse(null, duration)
            } else {
                val exitCode = process.exitValue()
                logger.info("Finished executing $commandLine in $duration exit code $exitCode")
                // KT-65113: Looks like read() from stdout/stderr of a child process may hang on Windows
                // even when the child process is already terminated.
                val waitStreamsDuration = if (HostManager.hostIsMingw) 10.seconds else Duration.INFINITE
                try {
                    withTimeout(waitStreamsDuration) {
                        streams.drain()
                    }
                } catch (e: TimeoutCancellationException) {
                    logger.warning("Failed to join the streams in $waitStreamsDuration.")
                    cancel()
                }
                ExecuteResponse(exitCode, duration)
            }
        }
    }
}