/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

private class Step(val command: String, val body: List<String>)

class LldbSessionSpecification private constructor(
    private val steps: List<Step>,
) {


    fun generateCLIArguments(pp: File): List<String> {
        val args = mutableListOf("-b", "-o", "command script import ${pp.absolutePath}")
        args.addAll(steps.flatMap { listOf("-o", it.command) })
        return args
    }

    // TODO: Re-introduce this when we add support for iOS simulator based testing.
    fun targetIsHost() = true

    fun matchOutput(output: String): Boolean {
        val blocks = output.split(lldbOutputCommand).filterNot(String::isEmpty)
        if (targetIsHost()) {
            check(blocks[0].startsWith("(lldb) target create")) { "Missing block \"target create\". Got: ${blocks[0]}" }
            check(blocks[1].startsWith("(lldb) command script import")) {
                "Missing block \"command script import\". Got: ${blocks[1]}"
            }
        }
        val responses = if (targetIsHost())
            blocks.drop(2)
        else
            blocks.drop(2).dropLast(1)

        val recordedSteps = responses.map { Step(it.lines().first(), it.lines().drop(1)) }

        if (recordedSteps.size != steps.size) {
            val message = """
                |Responses do not match commands.
                |
                |COMMANDS: |${steps.map { it.command }} (${steps.size})
                |RESPONSES: |${recordedSteps.map { it.command }} (${recordedSteps.size})
            """.trimMargin()
            fail { message }
        }

        steps.zip(recordedSteps).all { (step, recordedStep) -> recordedStep.command == "(lldb) ${step.command}" }

        for ((step, recordedStep) in steps.zip(recordedSteps)) {
            check(recordedStep.command == "(lldb) ${step.command}") {
                "Wrong command in response. Expected: (lldb) ${step.command}. Got: ${recordedStep.command}"
            }
            val mismatch = findMismatch(step.body, recordedStep.body)
            if (mismatch != null) {
                val message = """
                    |Wrong LLDB output.
                    |
                    |COMMAND: ${step.command}
                    |PATTERN: $mismatch
                    |OUTPUT:
                    |${recordedStep.body.joinToString("\n")}
                """.trimMargin()
                fail { message }
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
        check(indices == indices.sorted())
        return null
    }

    private fun match(pattern: String, line: String): Boolean {
        val chunks = pattern.split(wildcard)
            .filter { it.isNotBlank() }
            .map { it.trim() }
        check(chunks.isNotEmpty())
        val trimmedLine = line.trim()

        val indices = chunks.map { trimmedLine.indexOf(it) }
        if (indices.any { it == -1 } || indices != indices.sorted()) return false
        if (!(trimmedLine.startsWith(chunks.first()) || pattern.startsWith("[..]"))) return false
        if (!(trimmedLine.endsWith(chunks.last()) || pattern.endsWith("[..]"))) return false
        return true
    }

    companion object {
        val lldbOutputCommand = """(?=\(lldb\))""".toRegex()
        val wildcard = """\s*\[\.\.]\s*""".toRegex()
        val separator = "(?=^>)".toRegex(RegexOption.MULTILINE)

        fun parse(spec: String): LldbSessionSpecification {
            val blocks = spec.trimIndent()
                .split(separator)
                .filterNot(String::isEmpty)
            for (cmd in blocks) {
                check(cmd.startsWith(">")) { "Invalid lldb session specification: $cmd" }
            }
            val steps = blocks.map {
                Step(it.lines().first().substring(1).trim(), it.lines().drop(1).filter { it.isNotBlank() })
            }
            return LldbSessionSpecification(steps)
        }
    }
}
