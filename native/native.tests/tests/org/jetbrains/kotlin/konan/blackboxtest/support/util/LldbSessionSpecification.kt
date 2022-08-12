/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File

class LldbSessionSpecification private constructor(
    val commands: List<String>,
    val patterns: List<List<String>>
) {

    fun generateCLIArguments(pp: File): List<String> {
        val args = mutableListOf("-b", "-o", "command script import ${pp.absolutePath}")
        args.addAll(commands.flatMap { listOf("-o", it) })
        return args
    }

    // TODO: Re-introduce this when we add support for iOS simulator based testing.
    fun targetIsHost() = true

    fun matchOutput(output: String): Boolean {
        val blocks = output.split("""(?=\(lldb\))""".toRegex()).filterNot(String::isEmpty)
        if (targetIsHost()) {
            check(blocks[0].startsWith("(lldb) target create")) { "Missing block \"target create\". Got: ${blocks[0]}" }
            check(blocks[1].startsWith("(lldb) command script import")) {
                "Missing block \"command script import\". Got: ${blocks[0]}"
            }
        }
        val responses = if (targetIsHost())
            blocks.drop(2)
        else
            blocks.drop(2).dropLast(1)

        val executedCommands = responses.map { it.lines().first() }
        val bodies = responses.map { it.lines().drop(1) }
        val responsesMatch = executedCommands.size == commands.size
                && commands.zip(executedCommands).all { (cmd, h) -> h == "(lldb) $cmd" }

        if (!responsesMatch) {
            val message = """
                |Responses do not match commands.
                |
                |COMMANDS: |$commands (${commands.size})
                |RESPONSES: |$executedCommands (${executedCommands.size})
                |
                |FULL SESSION:
                |$output
            """.trimMargin()
            fail{ message }
        }

        for ((patternBody, command) in patterns.zip(bodies).zip(executedCommands)) {
            val (pattern, body) = patternBody
            val mismatch = findMismatch(pattern, body)
            if (mismatch != null) {
                val message = """
                    |Wrong LLDB output.
                    |
                    |COMMAND: $command
                    |PATTERN: $mismatch
                    |OUTPUT:
                    |${body.joinToString("\n")}
                    |
                    |FULL SESSION:
                    |$output
                """.trimMargin()
                fail{ message }
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
        val chunks = pattern.split("""\s*\[\.\.]\s*""".toRegex())
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
        fun parse(spec: String): LldbSessionSpecification {
            val blocks = spec.trimIndent()
                .split("(?=^>)".toRegex(RegexOption.MULTILINE))
                .filterNot(String::isEmpty)
            for (cmd in blocks) {
                check(cmd.startsWith(">")) { "Invalid lldb session specification: $cmd" }
            }
            val commands = blocks.map { it.lines().first().substring(1).trim() }
            val patterns = blocks.map { it.lines().drop(1).filter { it.isNotBlank() } }
            return LldbSessionSpecification(commands, patterns)
        }
    }
}
