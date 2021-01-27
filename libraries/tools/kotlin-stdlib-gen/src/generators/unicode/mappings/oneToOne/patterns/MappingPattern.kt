/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.patterns

/**
 * A range of consequent chars that fit within a particular mapping pattern.
 */
internal interface MappingPattern {
    /**
     * Char code of the first char in this range.
     */
    val start: Int

    /**
     * Char code of the last char in this range.
     */
    val end: Int

    /**
     * Appends the [charCode] to this range pattern.
     * Returns true if the [charCode] with the specified [categoryCode] and [mapping] was accommodated within this pattern.
     * Returns false otherwise.
     *
     * @param mapping the difference between the [charCode] and the char it converts to.
     */
    fun append(charCode: Int, categoryCode: String, mapping: Int): Boolean
}

internal val MappingPattern.length: Int get() = end - start + 1