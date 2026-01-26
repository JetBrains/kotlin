/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import org.jetbrains.kotlin.platform.wasm.BinaryenConfig
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.ir.WasmBinaryData
import org.jetbrains.kotlin.wasm.ir.WasmBinaryData.Companion.writeTo
import java.io.File
import kotlin.test.fail

sealed interface WasmOptimizer {
    fun run(wasmInput: WasmBinaryData, withText: Boolean = false, closedWorld: Boolean = true): OptimizationResult

    data class OptimizationResult(val wasm: WasmBinaryData, val wat: String?)

    object Binaryen : WasmOptimizer {
        private val binaryenPath = System.getProperty("binaryen.path")

        //TODO: Replace it to separate arguments property in org.jetbrains.kotlin.platform.wasm.BinaryenConfig
        private val binaryenArgsClosedWorld = listOf(
            "--enable-gc",
            "--enable-reference-types",
            "--enable-exception-handling",
            "--enable-bulk-memory",
            "--enable-nontrapping-float-to-int",
            "--no-inline=kotlin.wasm.internal.throwValue",
            "--no-inline=kotlin.wasm.internal.getKotlinException",
            "--no-inline=kotlin.wasm.internal.jsToKotlinStringAdapter",
            "--inline-functions-with-loops",
            "--traps-never-happen",
            "--fast-math",
            "--type-ssa",
            "-O3",
            "-O3",
            "-O3",
        )

        override fun run(wasmInput: WasmBinaryData, withText: Boolean, closedWorld: Boolean): OptimizationResult {
            val args = if (closedWorld) BinaryenConfig.binaryenArgs else binaryenArgsClosedWorld

            val command = arrayOf(binaryenPath, *args.toTypedArray())
            return OptimizationResult(
                exec(command, wasmInput).let { WasmBinaryData(it, it.size) },
                runIf(withText) { exec(command + "-S", wasmInput).toString(Charsets.UTF_8) }
            )
        }

        private fun exec(command: Array<String>, input: WasmBinaryData): ByteArray {
            var savedInput: File? = null
            var savedOutput: File? = null
            try {
                val timestamp = System.currentTimeMillis()

                savedInput = File
                    .createTempFile("binaryen_input_$timestamp", ".wasm")
                    .apply { input.writeTo(this) }

                savedOutput = File.createTempFile("binaryen_output_$timestamp", ".wasm")

                val processBuilder = ProcessBuilder(*(command + listOf("-o", savedOutput.absolutePath, savedInput.absolutePath)))
                    .redirectErrorStream(true)
                val process = processBuilder.start()

                val output = StringBuilder()
                val reader = process.inputStream.bufferedReader()
                while (process.isAlive) {
                    output.append(reader.readText())
                }

                val exitValue = process.exitValue()

                if (exitValue != 0) {
                    val commandString = processBuilder.command().joinToString(" ") { escapeShellArgument(it) }
                    fail("Command \"$commandString\" terminated with exit code $exitValue\nOUTPUT:\n$output")
                }

                return savedOutput.readBytes()
            } finally {
                savedInput?.delete()
                savedOutput?.delete()
            }
        }
    }
}