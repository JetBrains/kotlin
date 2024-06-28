/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

private class Step(val command: String, val body: List<String>) {
    companion object {
        fun parse(block: String, expectedPrefix: String): Step {
            val lines = block.lines()

            val commandWithPrefix = lines.first()
            assertTrue(commandWithPrefix.startsWith(expectedPrefix)) {
                "The command should start with $expectedPrefix. Got: $commandWithPrefix"
            }

            val command = commandWithPrefix.removePrefix(expectedPrefix).trimStart()
            val body = lines.drop(1).filterNot(String::isBlank)

            return Step(command, body)
        }
    }
}

internal class LLDBSessionSpec private constructor(private val expectedSteps: List<Step>) {
    fun generateCLIArguments(prettyPrinters: File): List<String> = buildList {
        this += "-b"
        this += "-o"
        this += "command script import ${prettyPrinters.absolutePath}"
        expectedSteps.forEach { step ->
            this += "-o"
            this += step.command
        }
    }

    fun checkLLDBOutput(output: String, nativeTargets: KotlinNativeTargets): Boolean {
        val blocks = output.split(LLDB_OUTPUT_SEPARATOR).filterNot(String::isBlank)

        val meaningfulBlocks = if (nativeTargets.testTarget == nativeTargets.hostTarget) {
            // TODO: why are these two leading blocks only checked for the host target?

            val createTargetBlock = blocks.getOrElse(0) { "" }
            assertTrue(createTargetBlock.startsWith("(lldb) target create")) {
                "Missing block \"target create\". Got: $createTargetBlock"
            }

            val commandScriptBlock = blocks.getOrElse(1) { "" }
            assertTrue(commandScriptBlock.startsWith("(lldb) command script import")) {
                "Missing block \"command script import\". Got: $commandScriptBlock"
            }

            blocks.drop(2)
        } else {
            blocks.drop(2).dropLast(1)
        }

        val recordedSteps = meaningfulBlocks.map { block -> Step.parse(block, LLDB_COMMAND_PREFIX) }
        assertTrue(expectedSteps.size == recordedSteps.size) {
            """
                The number of responses do not match the number of commands.
                - Commands (${expectedSteps.size}): ${expectedSteps.map { it.command }}
                - Responses (${recordedSteps.size}): ${recordedSteps.map { it.command }}
            """.trimIndent()
        }

        for ((expectedStep, recordedStep) in expectedSteps.zip(recordedSteps)) {
            assertTrue(expectedStep.command == recordedStep.command) {
                """
                    Wrong command in response.
                    - Expected: ${expectedStep.command}
                    - Actual: ${recordedStep.command}
                """.trimIndent()
            }

            val mismatch = findMismatch(expectedStep.body, recordedStep.body)
            if (mismatch != null) {
                fail {
                    buildString {
                        appendLine("Wrong LLDB output.")
                        append("- Command: ").appendLine(expectedStep.command)
                        append("- Expected (pattern): ").appendLine(mismatch)
                        appendLine("- Actual:")
                        recordedStep.body.joinTo(this, separator = "\n")
                    }
                }
            }
        }
        return true
    }

    private fun findMismatch(patterns: List<String>, actualLines: List<String>): String? {
        val indices = mutableListOf<Int>()
        for (pattern in patterns) {
            val idx = actualLines.indexOfFirst { match(pattern, it) }
            if (idx == -1) {
                return pattern
            }
            indices += idx
        }
        assertTrue(indices == indices.sorted())
        return null
    }

    private fun match(pattern: String, line: String): Boolean {
        val chunks = pattern.split(LINE_WILDCARD)
            .filter { it.isNotBlank() }
            .map { it.trim() }
        assertTrue(chunks.isNotEmpty())
        val trimmedLine = line.trim()

        val indices = chunks.map { trimmedLine.indexOf(it) }
        if (indices.any { it == -1 } || indices != indices.sorted()) return false
        if (!(trimmedLine.startsWith(chunks.first()) || pattern.startsWith("[..]"))) return false
        if (!(trimmedLine.endsWith(chunks.last()) || pattern.endsWith("[..]"))) return false
        return true
    }

    companion object {
        private const val LLDB_COMMAND_PREFIX = "(lldb)"
        private const val SPEC_COMMAND_PREFIX = ">"

        private val LLDB_OUTPUT_SEPARATOR = """(?=\(lldb\))""".toRegex()
        private val SPEC_BLOCK_SEPARATOR = "(?=^>)".toRegex(RegexOption.MULTILINE)

        private val LINE_WILDCARD = """\s*\[\.\.]\s*""".toRegex()

        fun parse(lldbSpec: String): LLDBSessionSpec = LLDBSessionSpec(
            lldbSpec.split(SPEC_BLOCK_SEPARATOR)
                .filterNot(String::isBlank)
                .map { block -> Step.parse(block, SPEC_COMMAND_PREFIX) }
        )
    }
}
