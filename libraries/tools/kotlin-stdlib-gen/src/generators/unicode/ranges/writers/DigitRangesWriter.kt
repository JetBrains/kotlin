/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.writers

import generators.unicode.ranges.RangesWritingStrategy
import generators.unicode.writeIntArray
import java.io.FileWriter

internal class DigitRangesWriter(private val strategy: RangesWritingStrategy) : RangesWriter {
    override fun write(rangeStart: List<Int>, rangeEnd: List<Int>, rangeCategory: List<Int>, writer: FileWriter) {
        // digit ranges always have length equal to 10, so that the difference between the last char code in range and the first one is always 9.
        // Therefore, no need to generate ranges end
        check(rangeStart.indices.map { rangeEnd[it] - rangeStart[it] + 1 }.all { it > 0 && it % 10 == 0 })

        val splittedRangeStart = splitRangesBy10(rangeStart, rangeEnd)
        strategy.beforeWritingRanges(writer)
        writer.writeIntArray("rangeStart", splittedRangeStart, strategy)
        strategy.afterWritingRanges(writer)
        writer.appendLine()
        writer.appendLine(binarySearchRange())
        writer.appendLine()
        writer.appendLine(digitToIntImpl())
        writer.appendLine()
        writer.appendLine(isDigitImpl())
    }

    private fun splitRangesBy10(rangeStart: List<Int>, rangeEnd: List<Int>): List<Int> =
        rangeStart.indices.flatMap {
            rangeStart[it]..rangeEnd[it] step 10
        }

    private fun binarySearchRange(): String = """
        /**
         * Returns the index of the largest element in [array] smaller or equal to the specified [needle],
         * or -1 if [needle] is smaller than the smallest element in [array].
         */
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

    private fun digitToIntImpl(): String {
        val rangeStart = strategy.rangeRef("rangeStart")
        return """
        /**
         * Returns an integer from 0..9 indicating the digit this character represents,
         * or -1 if this character is not a digit.
         */
        internal fun Char.digitToIntImpl(): Int {
            val ch = this.code
            val index = binarySearchRange($rangeStart, ch)
            val diff = ch - $rangeStart[index]
            return if (diff < 10) diff else -1
        }
        """.trimIndent()
    }

    private fun isDigitImpl(): String {
        return """
        /**
         * Returns `true` if this character is a digit.
         */
        internal fun Char.isDigitImpl(): Boolean {
            return digitToIntImpl() >= 0
        }
        """.trimIndent()
    }
}