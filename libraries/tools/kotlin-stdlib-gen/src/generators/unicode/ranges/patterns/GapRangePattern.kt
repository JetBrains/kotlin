/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.patterns

import generators.unicode.ranges.writers.hex

/**
 * A range of consequent chars that starts with a letter and ends with a letter, and contains multiple ranges of consequent not-letter chars.
 *
 * All letter chars in this range have the same category id.
 *
 * @param charCode the start of this range
 * @param categoryId the category id of the char with the specified [charCode]
 * @param unassignedCategoryId the categoryId of the unassigned chars.
 *      Chars that are not appended or prepended are considered to be unassigned
 * @param makeCategory the function used to transform this range to an Int representation that is returned from the [category] function.
 */
internal class GapRangePattern private constructor(
    charCode: Int,
    private val categoryId: String,
    private val unassignedCategoryId: String,
    private val makeCategory: (start: Int, end: Int, gaps: List<Gap>) -> Int
) : RangePattern {
    private val start: Int = charCode
    private var end: Int = charCode
    private val gaps = mutableListOf<Gap>()

    init {
        require(categoryId == "OL")
    }

    override fun append(charCode: Int, categoryId: String): Boolean {
        require(charCode > end)

        if (categoryId == unassignedCategoryId) {
            return true
        }

        if (categoryId != this.categoryId) {
            return false
        }

        // lll_gap_lll_X_l
        if (end == charCode - 1) {
            // _X_ is empty -> append the letter
            end = charCode
            return true
        }

        val newGap = Gap(start = end + 1, length = charCode - end - 1)
        val charsBeforeNewGap = newGap.start - if (gaps.isEmpty()) start else gaps.last().let { it.start + it.length }
        val bits = (gaps.size + 1) * (CHARS_BITS + GAP_BITS)

        if (isValid(charsBeforeNewGap, newGap.length) && bits <= TOTAL_BITS) {
            gaps.add(newGap)
            end = charCode
            return true
        }

        return false
    }

    override fun prepend(charCode: Int, categoryId: String): Boolean {
        assert(charCode < start)
        return false
    }

    override fun rangeStart(): Int {
        return start
    }

    override fun rangeEnd(): Int {
        return end
    }

    override fun category(): Int {
        return makeCategory(start, end, gaps)
    }

    override fun categoryIdOf(charCode: Int): String {
        require(charCode in start..end)
        for (gap in gaps) {
            if (charCode < gap.start) {
                return categoryId
            }
            if (charCode < gap.start + gap.length) {
                return unassignedCategoryId
            }
        }
        return categoryId
    }

    override fun toString(): String {
        return "GapPattern{" +
                "start=" + start.hex() +
                ", end=" + end.hex() +
                ", length=" + rangeLength() +
                ", gaps=" + gaps +
                ", categoryId=" + categoryId +
                "}"
    }

    companion object {
        internal const val CHARS_BITS = 7
        internal const val GAP_BITS = 7
        private const val TOTAL_BITS = 29

        internal data class Gap(val start: Int, val length: Int)

        fun from(
            range: RangePattern,
            charCode: Int,
            categoryId: String,
            unassignedCategoryId: String,
            makeCategory: (start: Int, end: Int, gaps: List<Gap>) -> Int
        ): RangePattern? {
            val start = range.rangeStart()
            val startCategoryId = range.categoryIdOf(start)

            check(startCategoryId != unassignedCategoryId)

            if (startCategoryId != categoryId || categoryId != "OL") return null

            val gapRange = GapRangePattern(start, startCategoryId, unassignedCategoryId, makeCategory)
            if (gapRange.append(start + 1, range.rangeEnd(), range::categoryIdOf, charCode, categoryId)) {
                return gapRange
            }
            return null
        }

        private fun isValid(charsBeforeGap: Int, gapLength: Int): Boolean {
            return charsBeforeGap < (1 shl CHARS_BITS) && gapLength < (1 shl GAP_BITS)
        }
    }
}
