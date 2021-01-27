/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.builders

internal class DigitRangesBuilder : RangesBuilder() {
    override fun categoryId(categoryCode: String): String {
        return categoryCode
    }

    override fun shouldSkip(categoryId: String): Boolean {
        return categoryId != CharCategory.DECIMAL_DIGIT_NUMBER.code
    }
}