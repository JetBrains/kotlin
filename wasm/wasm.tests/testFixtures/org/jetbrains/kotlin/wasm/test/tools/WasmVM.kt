/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Boolean.getBoolean
import java.security.MessageDigest
import kotlin.test.fail

private val toolLogsEnabled: Boolean = getBoolean("kotlin.js.test.verbose")

internal interface WasmVmDescriptor {
    val vmName: String
    val entryPointIsJsFile: Boolean
}

internal sealed class WasmVM(
    val property: String,
    override val entryPointIsJsFile: Boolean
) : WasmVmDescriptor {
    protected val tool = ExternalTool(System.getProperty(property))
    override val vmName: String
        get() = javaClass.simpleName

    abstract fun run(
        entryFile: String,
        jsFiles: List<String>,
        workingDirectory: File?,
        useNewExceptionHandling: Boolean = false,
        useStackSwitching: Boolean = false,
        toolArgs: List<String> = emptyList(),
    ): String

    object V8 : WasmVM(property = "javascript.engine.path.V8", entryPointIsJsFile = true) {
        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            useStackSwitching: Boolean,
            toolArgs: List<String>,
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                *jsFiles.toTypedArray(),
                "--module",
                *if (useNewExceptionHandling) arrayOf("--no-experimental-wasm-legacy-eh") else emptyArray(),
                *if (useStackSwitching) arrayOf("--experimental-wasm-wasmfx") else emptyArray(),
                entryFile,
                workingDirectory = workingDirectory,
            )
    }

    object SpiderMonkey : WasmVM(property = "javascript.engine.path.SpiderMonkey", entryPointIsJsFile = true) {
        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            useStackSwitching: Boolean,
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

    object JavaScriptCore : WasmVM(property = "javascript.engine.path.JavaScriptCore", entryPointIsJsFile = true) {
        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            useStackSwitching: Boolean,
            toolArgs: List<String>
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                *jsFiles.toTypedArray(),
                "--module-file=$entryFile",
                workingDirectory = workingDirectory,
            )
    }

    object WasmEdge : WasmVM(property = "wasm.engine.path.WasmEdge", entryPointIsJsFile = false) {
        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            useStackSwitching: Boolean,
            toolArgs: List<String>,
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                entryFile,
                // Either the grouped result-collecting driver or `wasiBoxTestRun.kt`'s box glue, never both — see
                // `assertDriverOwnsStartTestExport`.
                "startTest",
                workingDirectory = workingDirectory,
            )
    }

    object Wasmtime : WasmVM(property = "wasm.engine.path.Wasmtime", entryPointIsJsFile = false) {
        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            useStackSwitching: Boolean,
            toolArgs: List<String>,
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                "-W",
                "gc,function-references,exceptions",
                "--invoke",
                "startTest",
                entryFile,
                workingDirectory = workingDirectory,
            )
    }

    object NodeJs : WasmVM(property = "wasm.javascript.engine.path.NodeJs", entryPointIsJsFile = true) {
        override fun run(
            entryFile: String,
            jsFiles: List<String>,
            workingDirectory: File?,
            useNewExceptionHandling: Boolean,
            useStackSwitching: Boolean,
            toolArgs: List<String>
        ) =
            tool.run(
                *toolArgs.toTypedArray(),
                *if (useNewExceptionHandling) arrayOf("--no-experimental-wasm-legacy-eh", "--experimental-wasm-exnref") else emptyArray(),
                *jsFiles.toTypedArray(),
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

        // Drain process output without allowing an untrusted VM to grow the JVM heap without a bound.
        val stdout = BoundedOutputCapture()
        BufferedReader(InputStreamReader(process.inputStream)).use { bufferedStdout ->
            val buffer = CharArray(8 * 1024)
            while (true) {
                val count = bufferedStdout.read(buffer)
                if (count < 0) break
                stdout.append(buffer, count)
            }
        }

        val exitValue = process.waitFor()
        val capturedStdout = stdout.toString()
        if (exitValue != 0) {
            fail("Command \"$commandString\" terminated with exit code $exitValue in working dir \"$workingDirectory\"\nOUTPUT:\n$capturedStdout\n---")
        }

        return capturedStdout
    }
}

private const val MAX_CAPTURED_PROCESS_OUTPUT_LENGTH = 4 * 1024 * 1024
private const val CAPTURED_PROCESS_OUTPUT_PREFIX_LENGTH = MAX_CAPTURED_PROCESS_OUTPUT_LENGTH / 2
private const val CAPTURED_PROCESS_OUTPUT_SUFFIX_LENGTH =
    MAX_CAPTURED_PROCESS_OUTPUT_LENGTH - CAPTURED_PROCESS_OUTPUT_PREFIX_LENGTH

/** Keeps enough head and tail context for diagnostics while bounding output retained from an external VM. */
internal class BoundedOutputCapture {
    private val digest = MessageDigest.getInstance("SHA-256")
    private var totalLength = 0L
    private var fullOutput = StringBuilder()
    private var prefix: String? = null
    private var suffix = CharArray(CAPTURED_PROCESS_OUTPUT_SUFFIX_LENGTH)
    private var suffixStart = 0
    private var suffixSize = 0
    private var renderedOutput: String? = null

    fun append(buffer: CharArray, length: Int) {
        val chunk = String(buffer, 0, length)
        totalLength += chunk.length
        digest.update(chunk.toByteArray(Charsets.UTF_8))

        if (prefix == null) {
            if (fullOutput.length + chunk.length <= MAX_CAPTURED_PROCESS_OUTPUT_LENGTH) {
                fullOutput.append(chunk)
                return
            }

            prefix = fullOutput.substring(0, CAPTURED_PROCESS_OUTPUT_PREFIX_LENGTH)
            appendToSuffix(fullOutput.substring(CAPTURED_PROCESS_OUTPUT_PREFIX_LENGTH))
            fullOutput = StringBuilder()
        }
        appendToSuffix(chunk)
    }

    private fun appendToSuffix(text: String) {
        for (character in text) {
            if (suffixSize < suffix.size) {
                suffix[(suffixStart + suffixSize) % suffix.size] = character
                suffixSize++
            } else {
                suffix[suffixStart] = character
                suffixStart = (suffixStart + 1) % suffix.size
            }
        }
    }

    override fun toString(): String {
        renderedOutput?.let { return it }
        val output = prefix?.let { outputPrefix ->
            val hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            buildString(outputPrefix.length + suffixSize + 128) {
                append(outputPrefix)
                append('\n')
                append(GroupedTestsResultProtocol.LINE_PREFIX)
                append(GroupedTestsResultProtocol.SEP)
                append(GroupedTestsResultProtocol.OUTPUT_TRUNCATED)
                append(GroupedTestsResultProtocol.SEP)
                append("original length=").append(totalLength).append(" chars; SHA-256=").append(hash)
                append('\n')
                for (index in 0 until suffixSize) {
                    append(suffix[(suffixStart + index) % suffix.size])
                }
            }
        } ?: fullOutput.toString()
        renderedOutput = output
        return output
    }
}

internal fun escapeShellArgument(arg: String): String =
    "'${arg.replace("'", "'\\''")}'"
