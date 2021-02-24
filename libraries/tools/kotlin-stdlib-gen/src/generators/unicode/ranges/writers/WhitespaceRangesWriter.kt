/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.writers

import java.io.FileWriter

internal class WhitespaceRangesWriter : RangesWriter {
    override fun write(rangeStart: List<Int>, rangeEnd: List<Int>, rangeCategory: List<Int>, writer: FileWriter) {
        writer.appendLine(isWhitespaceImpl(rangeStart, rangeEnd))
    }

    private fun isWhitespaceImpl(rangeStart: List<Int>, rangeEnd: List<Int>): String {
        val checks = rangeChecks(rangeStart, rangeEnd, "ch")
        return """
        /**
         * Returns `true` if this character is a whitespace.
         */
        internal fun Char.isWhitespaceImpl(): Boolean {
            val ch = this.toInt()
            return $checks
        }
        """.trimIndent()
    }

    private fun rangeChecks(rangeStart: List<Int>, rangeEnd: List<Int>, ch: String): String {
        val tab = "    "
        var tabCount = 5
        val builder = StringBuilder()

        for (i in rangeStart.indices) {
            if (i != 0) {
                builder.append(tab.repeat(tabCount)).append("|| ")
            }

            val start = rangeStart[i]
            val end = rangeEnd[i]
            when (start) {
                end -> {
                    if (start > 0x1000 && tabCount == 5) {
                        builder.appendLine("$ch > 0x1000 && (")
                        tabCount = 6
                        builder.append(tab.repeat(tabCount))
                    }
                    builder.appendLine("$ch == ${start.hex()}")
                }
                end - 1 -> {
                    builder.appendLine("$ch == ${start.hex()}")
                    builder.append(tab.repeat(tabCount)).append("|| ")
                    builder.appendLine("$ch == ${end.hex()}")
                }
                else -> {
                    builder.appendLine("$ch in ${start.hex()}..${end.hex()}")
                }
            }
        }

        return builder.append(tab.repeat(5)).append(")").toString()
    }
}