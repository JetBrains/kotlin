/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.model.ObjectFactory
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandleBuilder
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandleBuilder.Companion.execHandleBuilder
import org.jetbrains.kotlin.gradle.utils.processes.ExecResult
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

internal fun ServiceRegistry.execWithProgress(
    description: String,
    objects: ObjectFactory,
    readStdErr: Boolean = false,
    body: (ExecHandleBuilder) -> Unit,
): ExecResult {
    val stderr = ByteArrayOutputStream()
    val stdout = StringBuilder()
    val stdInPipe = PipedInputStream()
    val processRunnerBuilder = objects.execHandleBuilder {
        displayName = description
        standardOutput = PipedOutputStream(stdInPipe)
        redirectErrorStream = readStdErr
        ignoreExitValue = true
        body(this)
    }
    val processRunner = processRunnerBuilder.build()
    return operation(description) {
        progress(description)
        val outputReaderThread = thread(name = "output reader for [$description]") {
            stdInPipe.reader().use { reader ->
                val buffer = StringBuilder()
                while (true) {
                    val read = reader.read()
                    if (read == -1) break
                    val ch = read.toChar()
                    if (ch == '\b' || ch == '\n' || ch == '\r') {
                        if (buffer.isNotEmpty()) {
                            val str = buffer.toString()
                            stdout.append(str)
                            progress(str.trim())
                            buffer.setLength(0)
                        }
                        stdout.append(ch)
                    } else {
                        buffer.append(ch)
                    }
                }
            }
        }
        val result = processRunner.execute()
        outputReaderThread.join()
        if (result.exitValue != 0) {
            error(
                """
                Process '$description' returns ${result.exitValue}
                $stderr
                $stdout
                """.trimIndent()
            )
        }
        result
    }
}

internal fun ServiceRegistry.execWithErrorLogger(
    description: String,
    objects: ObjectFactory,
    body: (ExecHandleBuilder, ProgressLogger) -> Pair<TeamCityMessageCommonClient, TeamCityMessageCommonClient>,
): ExecResult {
    return operation(description) {
        progress(description)
        val processHandleBuilder = objects.execHandleBuilder {
            displayName = description
        }
        val (standardClient, errorClient) = body(processHandleBuilder, this@operation)
        processHandleBuilder.ignoreExitValue = true
        val processHandle = processHandleBuilder.build()
        val result = processHandle.execute()
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
