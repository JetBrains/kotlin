/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)

package org.jetbrains.kotlin.native.executors

import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.time.*

class ProcessStreams(
    process: Process,
    stdin: InputStream,
    stdout: OutputStream,
    stderr: OutputStream,
    jobLauncher: (suspend () -> Unit) -> Job,
) {
    private val ignoreIOErrors = AtomicBoolean(false)
    private val stdin = jobLauncher {
        stdin.apply {
            copyStreams(this, process.outputStream)
            close()
        }
        process.outputStream.close()
    }
    private val stdout = jobLauncher {
        stdout.apply {
            copyStreams(process.inputStream, this)
            close()
        }
        process.inputStream.close()
    }
    private val stderr = jobLauncher {
        stderr.apply {
            copyStreams(process.errorStream, this)
            close()
        }
        process.errorStream.close()
    }

    private fun copyStreams(from: InputStream, to: OutputStream) {
        try {
            from.copyTo(to)
        } catch(e: IOException) {
            if (ignoreIOErrors.get())
                return
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

fun CoroutineScope.pumpStreams(
    process: Process,
    stdin: InputStream,
    stdout: OutputStream,
    stderr: OutputStream,
) = ProcessStreams(
    process,
    stdin,
    stdout,
    stderr,
) {
    launch {
        it()
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
        runBlocking(Dispatchers.IO) {
            block(process)
        }
    } finally {
        // Make sure the process is killed even if the current thread was interrupted.
        // e.g. gradle task execution was cancelled by the user pressing ^C
        process.destroyForcibly()
        // The process is dead, no need to ensure its destruction during the shutdown.
        ProcessKiller.deregister(process)
    }
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
            val streams = pumpStreams(process, request.stdin, request.stdout, request.stderr)
            val (isTimeout, duration) = measureTimedValue {
                !process.waitFor(request.timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            }
            if (isTimeout) {
                logger.warning("Timeout running $commandLine in $duration")
                streams.cancel()
                process.destroyForcibly()
                streams.drain()
                ExecuteResponse(null, duration)
            } else {
                logger.info("Finished executing $commandLine in $duration exit code ${process.exitValue()}")
                streams.drain()
                ExecuteResponse(process.exitValue(), duration)
            }
        }
    }
}