/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)

package org.jetbrains.kotlin.native.executors

import java.io.File
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * [Executor] that does not run the process and immediately returns a successful response.
 *
 * Can be used for "compile-only" tests that don't need to actually execute the binary.
 *
 * @param explanation optional explanation for skipping execution for the logs
 */
class NoOpExecutor(
        private var explanation: String? = null,
) : Executor {
    private val logger = Logger.getLogger(NoOpExecutor::class.java.name)

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile
        logger.info("""
            |Skipping execution${explanation?.let { " ($it)" } ?: ""}
            |Command: ${request.executableAbsolutePath}${request.args.joinToString(separator = " ", prefix = " ")}
            |In working directory: ${workingDirectory.absolutePath}
            |With additional environment: ${request.environment.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\": \"${it.value}\"" }}
        """.trimMargin())
        return ExecuteResponse(exitCode = 0, executionTime = Duration.ZERO)
    }
}