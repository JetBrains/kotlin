/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.builders

import generators.unicode.ranges.patterns.RangePattern

internal class DigitRangesBuilder : RangesBuilder() {
    override fun categoryId(categoryCode: String): String {
        return categoryCode
    }

    override fun shouldSkip(categoryId: String): Boolean {
        return categoryId != CharCategory.DECIMAL_DIGIT_NUMBER.code
    }

    override val makeOnePeriodCategory: (Array<String>) -> Int
        get() = { 0 }

    override fun evolveLastRange(lastRange: RangePattern, charCode: Int, categoryId: String): RangePattern? {
        return null
    }
}