/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.patterns

import generators.unicode.ranges.writers.hex

/**
 * A range of consequent chars.
 *
 * The chars in the range may have periodic categories, e.g., [Lu, Ll, Lu, Ll, ...].
 *
 * @param charCode the start of this range
 * @param categoryId the category id of the char with the specified [charCode]
 * @param sequenceLength the maximum length this range can have.
 *      If [isPeriodic] is true than this range can be longer with:
 *      for every `charCode >= start + sequenceLength` categoryIdOf(charCode) is equal to categoryIdOf(charCode - sequenceLength)
 * @param isPeriodic true if this range is a periodic range with period [sequenceLength]
 * @param unassignedCategoryId the categoryId of the unassigned chars.
 *      Chars that are not appended or prepended are considered to be unassigned
 * @param makeCategory the function used to transform this range to an Int representation that is returned from the [category] function.
 *      [makeCategory] is called with an array having its size equal to `minOf(sequenceLength, rangeLength())`.
 */
internal class PeriodicRangePattern private constructor(
    charCode: Int,
    categoryId: String,
    val sequenceLength: Int,
    isPeriodic: Boolean,
    unassignedCategoryId: String,
    private val makeCategory: (Array<String>) -> Int
) : RangePattern {
    private var start: Int = charCode
    private var end: Int = charCode
    private val bag: Bag = Bag(sequenceLength, isPeriodic, unassignedCategoryId)

    init {
        bag.fill(charCode, categoryId)
    }

    override fun append(charCode: Int, categoryId: String): Boolean {
        require(charCode > end)
        if (!bag.fill(end + 1, charCode - 1, { bag.unassignedCategoryId }, charCode, categoryId)) {
            return false
        }
        end = charCode
        return true
    }

    override fun prepend(charCode: Int, categoryId: String): Boolean {
        require(charCode < start)
        if (!bag.fill(charCode + 1, start - 1, { bag.unassignedCategoryId }, charCode, categoryId)) {
            return false
        }
        start = charCode
        return true
    }

    override fun rangeStart(): Int {
        return start
    }

    override fun rangeEnd(): Int {
        return end
    }

    override fun category(): Int {
        return makeCategory(orderedCategoryIds())
    }

    private fun orderedCategoryIds(): Array<String> {
        val size = minOf(sequenceLength, rangeLength())
        return Array(size) { categoryIdOf(start + it) }
    }

    override fun categoryIdOf(charCode: Int): String {
        if (charCode !in start..end) {
            throw IllegalArgumentException("Char code ${charCode.hex()} is not in $this")
        }
        val categoryId = bag.categoryIdOf(charCode)
        check(categoryId != null)
        return categoryId
    }

    override fun toString(): String {
        return "PeriodicRangePattern{" +
                "start=" + start.hex() +
                ", end=" + end.hex() +
                ", length=" + rangeLength() +
                ", orderedCategoryIds=" + orderedCategoryIds().contentToString() +
                ", bag=" + bag +
                "}"
    }

    companion object {
        fun from(
            range: RangePattern,
            charCode: Int,
            categoryId: String,
            sequenceLength: Int,
            isPeriodic: Boolean,
            unassignedCategoryId: String,
            makeCategory: (Array<String>) -> Int
        ): PeriodicRangePattern? {
            require(charCode > range.rangeEnd())

            val start = range.rangeStart()
            val newRange = from(start, range.categoryIdOf(start), sequenceLength, isPeriodic, unassignedCategoryId, makeCategory)
            if (newRange.append(start + 1, range.rangeEnd(), range::categoryIdOf, charCode, categoryId)) {
                return newRange
            }
            return null
        }

        fun from(
            charCode: Int,
            categoryId: String,
            sequenceLength: Int,
            isPeriodic: Boolean,
            unassignedCategoryId: String,
            makeCategory: (Array<String>) -> Int
        ): PeriodicRangePattern {
            return PeriodicRangePattern(charCode, categoryId, sequenceLength, isPeriodic, unassignedCategoryId, makeCategory)
        }
    }
}

/**
 * A set of chars with their corresponding categories.
 *
 * Category Id of a char with code equal to `charCode` is placed at index `charCode % sequenceLength` of the [categoryIds].
 */
private class Bag(
    private val sequenceLength: Int,
    private val isPeriodic: Boolean,
    val unassignedCategoryId: String
) {
    private val categoryIds = arrayOfNulls<String>(sequenceLength)

    fun categoryIdOf(charCode: Int): String? {
        return categoryIds[charCode % sequenceLength]
    }

    /**
     * Returns true if a range with the specified [rangeStart], [rangeEnd] and [categoryIdOf] was successfully added
     * together with a char with the specified [charCode] and [categoryId].
     *
     * The [charCode] must go immediately after the [rangeEnd] or before the [rangeStart].
     */
    fun fill(rangeStart: Int, rangeEnd: Int, categoryIdOf: (Int) -> String, charCode: Int, categoryId: String): Boolean {
        require(charCode == rangeStart - 1 || charCode == rangeEnd + 1)

        val attempt = categoryIds.copyOf()

        for (ch in rangeStart..rangeEnd) {
            if (!attempt.fill(ch, categoryIdOf(ch))) return false
        }
        if (!attempt.fill(charCode, categoryId)) return false

        attempt.copyInto(categoryIds)
        return true
    }

    /**
     * Returns true if the [charCode] with the [categoryId] was successfully placed in [categoryIds].
     */
    fun fill(charCode: Int, categoryId: String): Boolean {
        return categoryIds.fill(charCode, categoryId)
    }

    /**
     * Returns true if the [charCode] with the [categoryId] was successfully placed in this array.
     *
     * The [charCode] is placed at index `charCode % sequenceLength`.
     */
    private fun Array<String?>.fill(charCode: Int, categoryId: String): Boolean {
        val index = charCode % sequenceLength
        val current = this[index]
        if (current == null || (isPeriodic && current == categoryId)) {
            this[index] = categoryId
            return true
        }
        return false
    }

    override fun toString(): String {
        return "Bag{" +
                "sequenceLength=" + sequenceLength +
                ", isPeriodic=" + isPeriodic +
                ", unassignedCategoryId=" + unassignedCategoryId +
                ", categoryIds=" + categoryIds.contentToString() +
                "}"
    }
}
