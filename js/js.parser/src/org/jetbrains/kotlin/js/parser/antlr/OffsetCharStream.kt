/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.CharStream

/**
 * A [CharStream] that offsets line and column numbers by the given amount.
 */
internal class OffsetCharStream(
    private val delegate: CharStream,
    private val startLine: Int,
    private val startColumn: Int
) : CharStream by delegate {

    private var currentLine = startLine
    private var currentColumn = startColumn
    private var position = 0

    override fun consume() {
        val ch = delegate.LA(1)
        delegate.consume()

        when (ch) {
            '\r'.code -> {
                // Check if next character is \n (CRLF sequence)
                if (delegate.LA(1) == '\n'.code) {
                    delegate.consume() // Skip the \n
                }
                // Fall through to newline handling
                currentLine++
                currentColumn = 0
            }
            '\n'.code, 0x2028, 0x2029 -> { // \n, Line Separator, Paragraph Separator
                currentLine++
                currentColumn = 0
            }
        }

        position++
    }

    override fun seek(index: Int) {
        if (index < position) {
            // Reset and recalculate
            delegate.seek(0)
            currentLine = startLine
            currentColumn = startColumn
            position = 0

            while (position < index) {
                consume()
            }
        } else {
            while (position < index) {
                consume()
            }
        }
    }

    // Override these to return custom positions for error reporting
    fun getLine(): Int = currentLine
    fun getCharPositionInLine(): Int = currentColumn
}
