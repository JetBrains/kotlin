/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.parser

import com.google.gwt.dev.js.rhino.CodePosition

class OffsetToSourceMapping(text: String) {
    private val data: IntArray

    init {
        var i = 0
        val lineSeparators = mutableListOf<Int>()
        lineSeparators += 0
        while (i < text.length) {
            val c = text[i++]
            val isNewLine = when (c) {
                '\r' -> {
                    if (i < text.length && text[i] == '\n') {
                        ++i
                    }
                    true
                }
                '\n' -> true
                else -> false
            }
            if (isNewLine) {
                lineSeparators += i
            }
        }

        data = lineSeparators.toIntArray()
    }

    operator fun get(offset: Int): CodePosition {
        val lineNumber = data.binarySearch(offset).let { if (it >= 0) it else -it - 2 }
        return CodePosition(lineNumber, offset - data[lineNumber])
    }
}