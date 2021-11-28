/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.builders

import generators.unicode.ranges.patterns.PeriodicRangePattern
import generators.unicode.ranges.patterns.RangePattern

internal class CharCategoryRangesBuilder : RangesBuilder() {

    override fun categoryId(categoryCode: String): String {
        return categoryCode
    }

    override fun shouldSkip(categoryId: String): Boolean {
        return false
    }

    override val makeOnePeriodCategory: (Array<String>) -> Int
        get() = ::periodPatternCategory

    override fun evolveLastRange(lastRange: RangePattern, charCode: Int, categoryId: String): RangePattern? {
        require(lastRange is PeriodicRangePattern)
        return when (lastRange.sequenceLength) {
            1 -> PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 2, isPeriodic = true, unassignedCategoryId, ::periodPatternCategory)
                ?: PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 3, isPeriodic = true, unassignedCategoryId, ::periodPatternCategory)
            2 -> PeriodicRangePattern.from(lastRange, charCode, categoryId, sequenceLength = 3, isPeriodic = true, unassignedCategoryId, ::periodPatternCategory)
            else -> null
        }
    }
}

// 17 and 31 category values are not reserved. Use 17 to replace UNASSIGNED value (0) to be able to encode range pattern categories.
internal const val UNASSIGNED_CATEGORY_VALUE_REPLACEMENT = 17
private val categoryCodeToValue = CharCategory.values().associateBy({ it.code }, { if (it.value == 0) UNASSIGNED_CATEGORY_VALUE_REPLACEMENT else it.value })

private fun periodPatternCategory(categoryIds: Array<String>): Int {
    // Each category value is <= 30, thus 5 bits is enough to represent it.
    var pattern = 0
    for (index in categoryIds.indices) {
        val value = categoryCodeToValue[categoryIds[index]]!!
        pattern = pattern or (value shl (5 * index))
    }
    return pattern
}