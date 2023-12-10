/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import java.io.File
import kotlin.time.measureTimedValue

internal fun codesign(file: File) : LoggedData.CompilationToolCall {
    val codesignTool = "/usr/bin/codesign"
    val processBuilder = ProcessBuilder(
        codesignTool,
        "--verbose", "-s", "-", file.absolutePath
    )
    processBuilder.environment()
    val process = processBuilder.start()
    val (exitCode, duration) = measureTimedValue {
        process.waitFor()
    }
    val errorOutput = process.errorStream.readBytes()
    val stdOutput = process.inputStream.readBytes()
    val parameters = CliToolLoggedData("CODESIGN", processBuilder.command())
    val loggedData = LoggedData.CompilationToolCall(
        toolName = "CODESIGN",
        parameters = parameters,
        exitCode = if (exitCode == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR,
        toolOutput = stdOutput.decodeToString() + errorOutput.decodeToString(),
        toolOutputHasErrors = errorOutput.isNotEmpty(),
        duration = duration
    )
    return loggedData
}