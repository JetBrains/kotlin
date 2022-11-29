/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gwt.dev.js.rhino

class CodePosition(val line: Int, val offset: Int) : Comparable<CodePosition> {
    override fun compareTo(other: CodePosition): Int =
            when {
                line < other.line -> -1
                line > other.line -> 1
                else -> offset.compareTo(other.offset)
            }

    override fun toString(): String = "($line, $offset)"
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

        if (Utils.isEndOfLine(c.code)) {
            offsetInLine = 0
            lineCount++
            assert(lineCount <= position.line)
        }
    }

    return length
}
