/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.writers

import generators.unicode.ranges.RangesWritingStrategy
import java.io.FileWriter

internal class DigitRangesWriter(private val strategy: RangesWritingStrategy) : RangesWriter {
    override fun write(rangeStart: List<Int>, rangeEnd: List<Int>, rangeCategory: List<Int>, writer: FileWriter) {
        // digit ranges always have length equal to 10, so that the difference between the last char code in range and the first one is always 9.
        // Therefore, no need to generate ranges end
        check(rangeStart.indices.all { rangeEnd[it] - rangeStart[it] == 9 })

        strategy.beforeWritingRanges(writer)
        writer.writeIntArray("rangeStart", rangeStart, strategy)
        strategy.afterWritingRanges(writer)
        writer.appendLine()
        writer.appendLine(binarySearchRange())
        writer.appendLine()
        writer.appendLine(isDigitImpl())
    }

    private fun binarySearchRange(): String = """
        internal fun binarySearchRange(array: IntArray, needle: Int): Int {
            var bottom = 0
            var top = array.size - 1
            var middle = -1
            var value = 0
            while (bottom <= top) {
                middle = (bottom + top) / 2
                value = array[middle]
                if (needle > value)
                    bottom = middle + 1
                else if (needle == value)
                    return middle
                else
                    top = middle - 1
            }
            return middle - (if (needle < value) 1 else 0)
        }
        """.trimIndent()

    private fun isDigitImpl(): String {
        val rangeStart = strategy.rangeRef("rangeStart")
        return """
        /**
         * Returns `true` if this character is a digit.
         */
        internal fun Char.isDigitImpl(): Boolean {
            val ch = this.toInt()
            val index = binarySearchRange($rangeStart, ch)
            val high = $rangeStart[index] + 9
            return ch <= high
        }
        """.trimIndent()
    }
}