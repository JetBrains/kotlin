/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Boolean.getBoolean
import kotlin.test.fail

private val toolLogsEnabled: Boolean = getBoolean("kotlin.js.test.verbose")

internal sealed class WasmVM(val shortName: String) {
    val name: String = javaClass.simpleName
    protected val tool = ExternalTool(System.getProperty("javascript.engine.path.$name"))

    abstract fun run(
        entryMjs: String,
        jsFiles: List<String>,
        workingDirectory: File?,
        disableExceptionHandlingIfPossible: Boolean = false,
        toolArgs: List<String> = emptyList()
    ): String

    object V8 : WasmVM("V8") {
        override fun run(
            entryMjs: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            disableExceptionHandlingIfPossible: Boolean,
            toolArgs: List<String>
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                *jsFiles.toTypedArray(),
                "--module",
                entryMjs,
                workingDirectory = workingDirectory,
            )
    }

    object SpiderMonkey : WasmVM("SM") {
        override fun run(
            entryMjs: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            disableExceptionHandlingIfPossible: Boolean,
            toolArgs: List<String>
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                "--wasm-verbose",
                *if (disableExceptionHandlingIfPossible) arrayOf("--no-wasm-exceptions") else emptyArray(),
                *jsFiles.flatMap { listOf("-f", it) }.toTypedArray(),
                "--module=$entryMjs",
                workingDirectory = workingDirectory,
            )
    }

    object NodeJs : WasmVM("NodeJs") {
        override fun run(
            entryMjs: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            disableExceptionHandlingIfPossible: Boolean,
            toolArgs: List<String>
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                "--experimental-wasm-gc",
                *jsFiles.flatMap { listOf("-f", it) }.toTypedArray(),
                entryMjs,
                workingDirectory = workingDirectory
            )
    }
}

internal class ExternalTool(val path: String) {
    fun run(vararg arguments: String, workingDirectory: File? = null): String {
        val command = arrayOf(path, *arguments)
        val processBuilder = ProcessBuilder(*command)
            .redirectErrorStream(true)

        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory)
        }

        val process = processBuilder.start()

        val commandString = command.joinToString(" ") { escapeShellArgument(it) }
        if (toolLogsEnabled) {
            println(
                if (workingDirectory != null) {
                    "(cd '$workingDirectory' && $commandString)"
                } else {
                    commandString
                }
            )
        }

        // Print process output
        val stdout = StringBuilder()
        val bufferedStdout = BufferedReader(InputStreamReader(process.inputStream))

        while (true) {
            val line = bufferedStdout.readLine() ?: break
            stdout.appendLine(line)
            println(line)
        }

        val exitValue = process.waitFor()
        if (exitValue != 0) {
            fail("Command \"$commandString\" terminated with exit code $exitValue")
        }

        return stdout.toString()
    }
}

internal fun escapeShellArgument(arg: String): String =
    "'${arg.replace("'", "'\\''")}'"
