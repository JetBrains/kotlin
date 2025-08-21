/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

private const val SINGLE_INDENT = "    "

class LineBuilder(private val contents: StringBuilder) {
    fun raw(string: String) {
        contents.append(string)
    }

    fun append(string: String) {
        require(!string.contains("\n")) {
            "The string cannot contain line breaks: this breaks indentation"
        }
        raw(string)
    }
}

class OutputFileBuilder {
    private val contents = StringBuilder()
    private var indentLevel = 0
    private var startedLine = false

    override fun toString(): String {
        check(indentLevel == 0) { "OutputFileBuilder rendered with non-zero indentLevel: $indentLevel" }
        return contents.toString()
    }

    fun indent(block: () -> Unit) {
        indentLevel++
        block()
        indentLevel--
    }

    fun line() {
        if (startedLine)
            return
        startedLine = true
        repeat(indentLevel) {
            contents.append(SINGLE_INDENT)
        }
    }

    fun line(block: LineBuilder.() -> Unit) {
        line()
        LineBuilder(contents).block()
    }

    fun line(text: String) = line {
        append(text)
    }

    fun lineEnd() {
        startedLine = false
        contents.appendLine()
    }

    fun lineEnd(block: LineBuilder.() -> Unit) {
        line(block)
        lineEnd()
    }

    fun lineEnd(text: String) = lineEnd {
        append(text)
    }

    fun raw(string: String) {
        contents.append(string)
    }
}