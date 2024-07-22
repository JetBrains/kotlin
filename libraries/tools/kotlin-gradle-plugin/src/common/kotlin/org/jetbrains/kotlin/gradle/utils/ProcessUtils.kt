/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Files
import java.util.LinkedList
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread


class RunProcessResult(
    var output: File,
    val returnCode: Int,
    val process: Process,
    private val command: List<String>,
) {
    /**
     * Write the full message to the logger, but trim the exception to prevent KT-66517
     */
    fun errorOnNonZeroExitCode(
        headerMessage: String?,
        logger: Logger,
    ) {
        if (returnCode == 0) return
        val errorMessage = buildString {
            headerMessage?.let {
                logger.error(it)
                appendLine(it)
            }

            val commandFailedMessage = "Executing of '${command.joinToString(" ")}' failed with code ${returnCode} and message:\n"
            logger.error(commandFailedMessage)
            appendLine(commandFailedMessage)

            val outputLimit = 100
            var hasOverflown = false
            val buffer = LinkedList<String>()
            output.reader().forEachLine { line ->
                logger.error(line)
                buffer.addLast(line)
                if (buffer.size > outputLimit) {
                    hasOverflown = true
                    buffer.removeFirst()
                }
            }
            if (hasOverflown) {
                appendLine("... last ${outputLimit} lines shown, see error log for the full output ...")
            }
            buffer.forEach { appendLine(it) }
        }
        error(errorMessage)
    }
}

internal fun runCommand(
    command: List<String>,
    logger: Logger,
    processConfiguration: ProcessBuilder.() -> Unit = { },
    onStdOutLine: ((line: String) -> Unit)? = null,
    onStdErrLine: ((line: String) -> Unit)? = null,
    errorOnNonZeroExitCode: Boolean = true,
): RunProcessResult {
    val result = assembleAndRunProcess(command, logger, processConfiguration)
    if (errorOnNonZeroExitCode && result.returnCode != 0) {
        result.errorOnNonZeroExitCode(
            headerMessage = null,
            logger = logger,
        )
    }
    return result
}

sealed class CommandFallback {
    data class Action(val fallback: String) : CommandFallback()
    data class Error(val error: String?) : CommandFallback()
}

internal fun runCommandWithFallback(
    command: List<String>,
    logger: Logger? = null,
    fallback: (result: RunProcessResult) -> CommandFallback,
    processConfiguration: ProcessBuilder.() -> Unit = { },
) {
    val runResult = assembleAndRunProcess(command, logger, processConfiguration)
    if (runResult.returnCode != 0) {
        when (val fallbackOption = fallback(runResult)) {
            is CommandFallback.Action -> fallbackOption.fallback
            is CommandFallback.Error -> error(fallbackOption.error ?: createErrorMessage(command, runResult))
        }
    }
}

private enum class StreamMessage {
    STDOUT,
    STDERR,
    STDOUT_EOF,
    STDERR_EOF
}

private fun assembleAndRunProcess(
    command: List<String>,
    logger: Logger? = null,
    processConfiguration: ProcessBuilder.() -> Unit = { },
    onStdOutLine: (line: String) -> Unit = {},
    onStdErrLine: (line: String) -> Unit = {},
): RunProcessResult {
    val process = ProcessBuilder(command).apply {
        this.processConfiguration()
    }.start()
    val temporaryDirectory = Files.createTempDirectory("${process.pid()}").toFile().apply { deleteOnExit() }

    logger?.info("Information about \"${command.joinToString(" ")}\" call:\n")

    val outputFile = temporaryDirectory.resolve("output")
    val queue = LinkedBlockingQueue<Pair<String, StreamMessage>>()
    val inputThread = thread {
        process.inputStream.use {
            it.reader().forEachLine { line ->
                queue.put(Pair(line, StreamMessage.STDOUT))
            }
        }
        queue.put(Pair("", StreamMessage.STDOUT_EOF))
    }
    val errorThread = thread {
        process.errorStream.use {
            it.reader().forEachLine { line ->
                queue.put(Pair(line, StreamMessage.STDERR))
            }
        }
        queue.put(Pair("", StreamMessage.STDERR_EOF))
    }

    var continueReadingStdout = true
    var continueReadingStderr = true
    outputFile.writer().use { writer ->
        while (continueReadingStdout || continueReadingStderr) {
            val (line, state) = queue.take()
            when (state) {
                StreamMessage.STDOUT_EOF -> {
                    continueReadingStdout = false
                }
                StreamMessage.STDERR_EOF -> {
                    continueReadingStderr = false
                }
                StreamMessage.STDOUT -> {
                    writer.appendLine(line)
                    onStdOutLine(line)
                    logger?.info(line)
                }
                StreamMessage.STDERR -> {
                    writer.appendLine(line)
                    onStdErrLine(line)
                    logger?.error(line)
                }
            }
        }
    }

    inputThread.join()
    errorThread.join()

    val retCode = process.waitFor()

    return RunProcessResult(
        outputFile,
        retCode,
        process,
        command,
    )
}