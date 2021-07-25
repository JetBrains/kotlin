/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.builders

import generators.unicode.ranges.patterns.PeriodicRangePattern
import generators.unicode.ranges.patterns.RangePattern
import generators.unicode.ranges.patterns.GapRangePattern

internal class LetterRangesBuilder : RangesBuilder() {

    override fun categoryId(categoryCode: String): String = when (categoryCode) {
        CharCategory.LOWERCASE_LETTER.code  -> categoryCode
        CharCategory.UPPERCASE_LETTER.code  -> categoryCode
        in letterCategoryCodes              -> "OL" // other letter
        else                                -> "NL" // not a letter
    }

    override fun shouldSkip(categoryId: String): Boolean {
        return categoryId == "NL"
    }

    override val makeOnePeriodCategory: (Array<String>) -> Int
        get() = ::periodPatternCategory

    override fun evolveLastRange(lastRange: RangePattern, charCode: Int, categoryId: String): RangePattern? {
        return when (lastRange) {
            is PeriodicRangePattern -> when (lastRange.sequenceLength) {
                1 ->
                    PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 2, isPeriodic = true, unassignedCategoryId, ::periodPatternCategory)
                        ?: GapRangePattern.from(lastRange, charCode, categoryId, unassignedCategoryId, ::gapPatternCategory)
                        ?: PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 15, isPeriodic = false, unassignedCategoryId, ::periodPatternCategory)
                2 ->
                    GapRangePattern.from(lastRange, charCode, categoryId, unassignedCategoryId, ::gapPatternCategory)
                        ?: PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 15, isPeriodic = false, unassignedCategoryId, ::periodPatternCategory)
                else -> null
            }
            is GapRangePattern ->
                PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 15, isPeriodic = false, unassignedCategoryId, ::periodPatternCategory)
            else ->
                error("Unreachable")
        }
    }
}

private val letterCategoryCodes = listOf(
    CharCategory.UPPERCASE_LETTER.code,
    CharCategory.LOWERCASE_LETTER.code,
    CharCategory.TITLECASE_LETTER.code,
    CharCategory.MODIFIER_LETTER.code,
    CharCategory.OTHER_LETTER.code
)

private fun bitmask(categoryId: String) = when (categoryId) {
    CharCategory.LOWERCASE_LETTER.code  -> 0b01
    CharCategory.UPPERCASE_LETTER.code  -> 0b10
    "OL"                                -> 0b11
    "NL"                                -> 0b00
    ""                                  -> 0b00
    else                                -> error("Unknown categoryID: $categoryId")
}

private fun periodPatternCategory(categoryIds: Array<String>): Int {
    var pattern = 0
    for (index in categoryIds.indices) {
        val value = bitmask(categoryIds[index])
        pattern = pattern or (value shl (2 * index))
    }
    pattern = pattern or (1 shl (2 * categoryIds.size))
    check(pattern and 0x3 != 0)
    return pattern
}

private fun gapPatternCategory(start: Int, @Suppress("UNUSED_PARAMETER") end: Int, gaps: List<GapRangePattern.Companion.Gap>): Int {
    var pattern = 0
    var shift = 2
    for (i in gaps.indices) {
        val gap = gaps[i]
        val charsBeforeGap = gap.start - if (i == 0) start else gaps[i - 1].let { it.start + it.length }
        pattern += charsBeforeGap shl shift
        shift += GapRangePattern.CHARS_BITS
        pattern += gap.length shl shift
        shift += GapRangePattern.GAP_BITS
    }
    check(pattern and 0x3 == 0)
    return pattern
}
