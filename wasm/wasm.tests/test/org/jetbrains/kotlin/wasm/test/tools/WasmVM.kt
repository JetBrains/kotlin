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
    open val property = "javascript.engine.path.$name"
    protected val tool by lazy(LazyThreadSafetyMode.NONE) {
        ExternalTool(System.getProperty(property))
    }

    abstract val hasJsSupport: Boolean

    abstract fun run(
        entryFile: String,
        jsFiles: List<String>,
        workingDirectory: File?,
        useNewExceptionHandling: Boolean = false,
        toolArgs: List<String> = emptyList(),
    ): String

    object V8 : WasmVM("V8") {
        override val hasJsSupport = true

        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            toolArgs: List<String>,
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                *jsFiles.toTypedArray(),
                "--module",
                *if (useNewExceptionHandling) arrayOf("--no-experimental-wasm-legacy-eh", "--experimental-wasm-exnref") else emptyArray(),
                entryFile,
                workingDirectory = workingDirectory,
            )
    }

    object SpiderMonkey : WasmVM("SM") {
        override val hasJsSupport = true

        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            toolArgs: List<String>,
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                "--wasm-verbose",
                *jsFiles.flatMap { listOf("-f", it) }.toTypedArray(),
                "--module=$entryFile",
                workingDirectory = workingDirectory,
            )
    }

    object WasmEdge : WasmVM("WE") {
        override val hasJsSupport = false
        override val property = "wasm.engine.path.WasmEdge"

        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            toolArgs: List<String>,
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                "--enable-gc",
                "--enable-exception-handling",
                "--enable-function-reference",
                entryFile,
                "__start",
                workingDirectory = workingDirectory,
            )
    }

    object NodeJs : WasmVM("NodeJs") {
        override val hasJsSupport = true

        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            toolArgs: List<String>
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                *jsFiles.flatMap { listOf("-f", it) }.toTypedArray(),
                entryFile,
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
