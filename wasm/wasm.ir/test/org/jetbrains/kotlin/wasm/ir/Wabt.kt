/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.test.fail

open class ExternalTool(private val path: String) {
    fun runAndPrint(vararg arguments: String) {
        val command = arrayOf(path, *arguments)
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        val commandString = command.joinToString(" ") { escapeShellArgument(it) }
        println(commandString)
        val inputStream: InputStream = process.inputStream
        val input = BufferedReader(InputStreamReader(inputStream))
        while (true) println(input.readLine() ?: break)

        val exitValue = process.waitFor()
        if (exitValue != 0) {
            fail("Command \"$commandString\" terminated with exit code $exitValue")
        }
    }
}

object Wabt {
    private val wabtBinPath = System.getProperty("wabt.bin.path")
    private val wasm2watTool = ExternalTool("$wabtBinPath/wasm2wat")
    private val wat2wasmTool = ExternalTool("$wabtBinPath/wat2wasm")
    private val wast2jsonTool = ExternalTool("$wabtBinPath/wast2json")

    fun wasm2wat(input: File, output: File) {
        wasm2watTool.runAndPrint("--enable-all", input.absolutePath, "-o", output.absolutePath)
    }

    fun wat2wasm(input: File, output: File) {
        wat2wasmTool.runAndPrint("--enable-all", input.absolutePath, "-o", output.absolutePath)
    }

    fun wast2json(input: File, output: File, vararg args: String) {
        wast2jsonTool.runAndPrint(*args, input.absolutePath, "-o", output.absolutePath)
    }
}

private fun escapeShellArgument(arg: String): String =
    "'${arg.replace("'", "'\\''")}'"
