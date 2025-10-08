/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser

fun Char.isEndOfLine(): Boolean {
    return this == '\r' || this == '\n' || this == '\u2028' || this == '\u2029'
}

/**
 * Calculates an offset from the start of a text for a position,
 * defined by line and offset in that line.
 */
fun String.offsetOf(position: CodePosition): Int {
    var i = 0
    var lineCount = 0
    var offsetInLine = 0

    while (i < length) {
        val c = this[i]

        if (lineCount == position.line && offsetInLine == position.offset) {
            return i
        }

        i++
        offsetInLine++

        if (c.isEndOfLine()) {
            offsetInLine = 0
            lineCount++
            assert(lineCount <= position.line)
        }
    }

    return length
}