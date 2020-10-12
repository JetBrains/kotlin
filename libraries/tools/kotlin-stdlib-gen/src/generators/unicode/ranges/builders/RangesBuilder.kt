/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.builders

import generators.unicode.ranges.patterns.PeriodicRangePattern
import generators.unicode.ranges.patterns.RangePattern
import generators.unicode.ranges.patterns.rangeLength

/**
 * The base class of character ranges builders.
 */
internal abstract class RangesBuilder {
    private val ranges = mutableListOf<RangePattern>()
    private var lastAppendedCharCode = -1

    /**
     * Appends a line from the UnicodeData.txt file.
     */
    fun append(char: String, name: String, categoryCode: String) {
        val charCode = char.toInt(radix = 16)
        val categoryId = categoryId(categoryCode)

        when {
            name.endsWith(", First>") -> rangeFirst(charCode, categoryId)
            name.endsWith(", Last>") -> rangeLast(charCode, categoryId)
            else -> append(charCode, categoryId)
        }

        lastAppendedCharCode = charCode
    }

    /**
     * Optimizes the number of ranges and returns them.
     *
     * Returns a [Triple] containing lists of range starts, ends and categories in that particular order.
     */
    fun build(): Triple<List<Int>, List<Int>, List<Int>> {
        for (code in lastAppendedCharCode + 1..0xffff) {
            appendSingleChar(code, unassignedCategoryId)
        }

        var index = ranges.lastIndex
        while (index > 0) {
            val previous = ranges[index - 1]
            val previousEnd = previous.rangeEnd()
            val previousEndCategory = previous.categoryIdOf(previousEnd)
            val current = ranges[index]
            if (current.prepend(previousEnd, previousEndCategory)) {
                val newPrevious = removeLast(previous)
                if (newPrevious != null) {
                    ranges[index - 1] = newPrevious
                } else {
                    ranges.removeAt(index - 1)
                    index--
                }
            } else {
                index--
            }
        }

//        if (this is LetterRangesBuilder) {
//            println(ranges.joinToString(separator = "\n"))
//        }

//        if (this is CharCategoryRangesBuilder) {
//            println(ranges.subList(fromIndex = 0, toIndex = 10).joinToString(separator = "\n"))
//        }

        return Triple(ranges.map { it.rangeStart() }, ranges.map { it.rangeEnd() }, ranges.map { it.category() })
    }

    /**
     * Appends the [charCode] as the start of a range of chars with the specified [categoryId].
     */
    private fun rangeFirst(charCode: Int, categoryId: String) {
        append(charCode, categoryId)
    }

    /**
     * Appends the [charCode] as the end of a range of chars with the specified [categoryId].
     * Chars between last appended char and the [charCode] are considered to have the specified [categoryId].
     */
    private fun rangeLast(charCode: Int, categoryId: String) {
        if (!shouldSkip(categoryId)) {
            check(ranges.last().rangeEnd() == lastAppendedCharCode)
            check(ranges.last().categoryIdOf(lastAppendedCharCode) == categoryId)
        }

        for (code in lastAppendedCharCode + 1..charCode) {
            appendSingleChar(code, categoryId)
        }
    }

    /**
     * Appends the [charCode] with the specified [categoryId].
     * Chars between last appended char and the [charCode] are considered to be unassigned.
     */
    private fun append(charCode: Int, categoryId: String) {
        for (code in lastAppendedCharCode + 1 until charCode) {
            appendSingleChar(code, unassignedCategoryId)
        }
        appendSingleChar(charCode, categoryId)
    }

    /**
     * Appends the [charCode] with the specified [categoryId] to the last range, or a new range containing the [charCode] is created.
     * The last range can be transformed to another range type to accommodate the [charCode].
     */
    private fun appendSingleChar(charCode: Int, categoryId: String) {
        if (shouldSkip(categoryId)) return

        if (ranges.isEmpty()) {
            ranges.add(createRange(charCode, categoryId))
            return
        }

        val lastRange = ranges.last()

        if (!lastRange.append(charCode, categoryId)) {
            val newLastRange = evolveLastRange(lastRange, charCode, categoryId)
            if (newLastRange != null) {
                ranges[ranges.lastIndex] = newLastRange
            } else {
                ranges.add(createRange(charCode, categoryId))
            }
        }
    }

    /**
     * Category id used for unassigned chars.
     */
    protected val unassignedCategoryId: String
        get() = categoryId(CharCategory.UNASSIGNED.code)


    /**
     * Creates the simplest range containing the single [charCode].
     */
    private fun createRange(charCode: Int, categoryId: String): RangePattern {
        return PeriodicRangePattern.from(charCode, categoryId, sequenceLength = 1, isPeriodic = true, unassignedCategoryId, makeOnePeriodCategory)
    }

    /**
     * Removes the last char in the specified [range].
     * Returns the simplest pattern that accommodated the remaining chars in the [range],
     * or `null` if the [range] contained a single char.
     */
    private fun removeLast(range: RangePattern): RangePattern? {
        if (range.rangeLength() == 1) {
            return null
        }

        val rangeStart = range.rangeStart()
        var result = createRange(rangeStart, range.categoryIdOf(rangeStart))
        for (code in rangeStart + 1 until range.rangeEnd()) {
            val categoryId = range.categoryIdOf(code)
            if (!shouldSkip(categoryId)) {
                result = if (result.append(code, categoryId)) result else evolveLastRange(result, code, categoryId)!!
            }
        }
        return result
    }

    /**
     * The id to use for the [categoryCode] - the Unicode general category code.
     */
    protected abstract fun categoryId(categoryCode: String): String

    /**
     * Returns true if this range builder skips chars with the specified [categoryId].
     */
    protected abstract fun shouldSkip(categoryId: String): Boolean

    /**
     * The function to use to transform periodic ranges with period equal to 1 to an Int representation.
     */
    protected abstract val makeOnePeriodCategory: (Array<String>) -> Int

    /**
     * Appends the [charCode] with the specified [categoryId] to the [lastRange] and returns the resulting range,
     * or returns `null` if [charCode] can't be appended to the [lastRange].
     * The [lastRange] can be transformed to another range type to accommodate the [charCode].
     */
    protected abstract fun evolveLastRange(
        lastRange: RangePattern,
        charCode: Int,
        categoryId: String
    ): RangePattern?
}