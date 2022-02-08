/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges.patterns

/**
 * A range of consequent chars that fit within a particular pattern.
 */
internal interface RangePattern {
    /**
     * Appends the [charCode] to this range pattern.
     * Returns true if the [charCode] with the specified [categoryId] could be accommodated within this pattern.
     * Returns false otherwise.
     */
    fun append(charCode: Int, categoryId: String): Boolean

    /**
     * Prepends the [charCode] to this range pattern.
     * Returns true if the [charCode] with the specified [categoryId] could be accommodated within this pattern.
     * Returns false otherwise.
     */
    fun prepend(charCode: Int, categoryId: String): Boolean

    /**
     * Char code of the first char in this range.
     */
    fun rangeStart(): Int

    /**
     * Char code of the last char in this range.
     */
    fun rangeEnd(): Int

    /**
     * An integer value that contains information about the category of each char in this range.
     */
    fun category(): Int

    /**
     * Returns category id of the char with the specified [charCode].
     * Throws an exception if the [charCode] is not in `rangeStart()..rangeEnd()`.
     */
    fun categoryIdOf(charCode: Int): String
}

internal fun RangePattern.rangeLength(): Int = rangeEnd() - rangeStart() + 1


internal fun RangePattern.append(rangeStart: Int, rangeEnd: Int, categoryIdOf: (Int) -> String, charCode: Int, categoryId: String): Boolean {
    for (code in rangeStart..rangeEnd) {
        if (!append(code, categoryIdOf(code))) {
            return false
        }
    }
    if (!append(charCode, categoryId)) {
        return false
    }
    return true
}
