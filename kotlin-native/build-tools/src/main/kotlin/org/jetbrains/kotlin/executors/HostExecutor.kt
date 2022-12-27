/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)

package org.jetbrains.kotlin.executors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.time.*

/**
 * [Executor] that runs the process on the host system.
 */
class HostExecutor : Executor {
    private val logger = Logger.getLogger(HostExecutor::class.java.name)

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        return runBlocking(Dispatchers.IO) {
            val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile
            val commandLine = "${request.executableAbsolutePath}${request.args.joinToString(separator = " ", prefix = " ")}"
            logger.info("""
                |Starting command: $commandLine
                |In working directory: ${workingDirectory.absolutePath}
                |With additional environment: ${request.environment.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\": \"${it.value}\"" }}
                |And timeout: ${request.timeout}
                """.trimMargin())
            val exitCodeWithTime = measureTimedValue {
                val process = ProcessBuilder(listOf(request.executableAbsolutePath) + request.args).apply {
                    directory(workingDirectory)
                    environment().putAll(request.environment)
                }.start()
                val jobs: MutableList<Job> = mutableListOf()
                jobs.add(launch {
                    request.stdin.apply {
                        copyTo(process.outputStream)
                        close()
                    }
                    process.outputStream.close()
                })
                jobs.add(launch {
                    request.stdout.apply {
                        process.inputStream.copyTo(this)
                        close()
                    }
                    process.inputStream.close()
                })
                jobs.add(launch {
                    request.stderr.apply {
                        process.errorStream.copyTo(this)
                        close()
                    }
                    process.errorStream.close()
                })

                if (!process.waitFor(request.timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                    logger.warning("Timeout running $commandLine")
                    // Cancel every stream, no need to wait for them.
                    jobs.forEach {
                        it.cancel()
                    }
                    process.destroyForcibly()
                    null
                } else {
                    // Drain every stream.
                    jobs.forEach {
                        it.join()
                    }
                    process.exitValue()
                }
            }
            ExecuteResponse(exitCodeWithTime.value, exitCodeWithTime.duration)
        }
    }
}