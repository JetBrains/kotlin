/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import org.jetbrains.kotlin.platform.wasm.BinaryenConfig
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import kotlin.test.fail

sealed interface WasmOptimizer {
    fun run(wasmInput: ByteArray, withText: Boolean = false): OptimizationResult

    data class OptimizationResult(val wasm: ByteArray, val wat: String?)

    object Binaryen : WasmOptimizer {
        private val binaryenPath = System.getProperty("binaryen.path")

        override fun run(wasmInput: ByteArray, withText: Boolean): OptimizationResult {
            val command = arrayOf(binaryenPath, *BinaryenConfig.binaryenArgs.toTypedArray())
            return OptimizationResult(
                exec(command, wasmInput),
                runIf(withText) { exec(command + "-S", wasmInput).toString(Charsets.UTF_8) }
            )
        }

        private fun exec(command: Array<String>, input: ByteArray): ByteArray {
            var savedInput: File? = null
            var savedOutput: File? = null
            try {
                val timestamp = System.currentTimeMillis()

                savedInput = File
                    .createTempFile("binaryen_input_$timestamp", ".wasm")
                    .apply { writeBytes(input) }

                savedOutput = File.createTempFile("binaryen_output_$timestamp", ".wasm")

                val processBuilder = ProcessBuilder(*(command + listOf("-o", savedOutput.absolutePath, savedInput.absolutePath)))
                    .redirectErrorStream(true)
                val exitValue = processBuilder.start().waitFor()

                if (exitValue != 0) {
                    val commandString = command.joinToString(" ") { escapeShellArgument(it) }
                    fail("Command \"$commandString\" terminated with exit code $exitValue")
                }

                return savedOutput.readBytes()
            } finally {
                savedInput?.delete()
                savedOutput?.delete()
            }
        }
    }
}