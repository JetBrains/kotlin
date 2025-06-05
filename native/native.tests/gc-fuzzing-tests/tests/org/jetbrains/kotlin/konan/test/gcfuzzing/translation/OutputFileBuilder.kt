/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

private const val SINGLE_INDENT = "    "

class OutputFileBuilder(private val defaultLineSuffix: String = "") {
    private val contents = StringBuilder()
    private var indentLevel = 0

    class LineBuilder(private val contents: StringBuilder) {
        fun append(string: String) {
            contents.append(string)
        }

        fun parens(vararg args: LineBuilder.() -> Unit) = try {
            append("(")
            args.forEachIndexed { index, arg ->
                arg()
                if (index < args.size - 1) append(", ")
            }
        } finally {
            append(")")
        }
    }

    override fun toString(): String {
        check(indentLevel == 0) { "OutputFileBuilder rendered with non-zero indentLevel: $indentLevel" }
        return contents.toString()
    }

    private fun appendIndent() {
        repeat(indentLevel) {
            contents.append(SINGLE_INDENT)
        }
    }

    private fun <R> indent(block: () -> R): R = try {
        indentLevel++
        block()
    } finally {
        indentLevel--
    }

    fun <R> line(suffix: String = defaultLineSuffix, lineBreak: Boolean = true, block: LineBuilder.() -> R): R = try {
        appendIndent()
        LineBuilder(contents).block()
    } finally {
        contents.append(suffix)
        if (lineBreak) contents.appendLine()
    }

    fun line(suffix: String = defaultLineSuffix, lineBreak: Boolean = true, text: String) = line(suffix, lineBreak) {
        append(text)
    }

    fun <R> braces(prefix: LineBuilder.() -> Unit, open: String = "{", close: String = "}", block: () -> R): R {
        line(suffix = "") {
            prefix()
            append(open)
        }
        return try {
            indent(block)
        } finally {
            line(suffix = "") { append(close) }
        }
    }

    fun <R> braces(prefix: String, open: String = "{", close: String = "}", block: () -> R): R = braces(
        prefix = { append(prefix) },
        open = open,
        close = close,
        block = block,
    )

    fun raw(string: String) {
        contents.appendLine(string)
    }
}