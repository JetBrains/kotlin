/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.patterns

import generators.unicode.toHexIntLiteral

/**
 * A range of consequent chars where all chars with code `start + k * distance` have the same [mapping] for case conversion.
 *
 * @param charCode the start of this range
 * @param categoryCode the category code of the char with the specified [charCode]
 * @param mapping the difference between [charCode] and the char it converts to.
 */
internal class EqualDistanceMappingPattern private constructor(
    charCode: Int,
    val categoryCode: String,
    val mapping: Int
) : MappingPattern {
    override val start = charCode
    override var end = charCode
        private set
    var distance = 1
        private set

    override fun append(charCode: Int, categoryCode: String, mapping: Int): Boolean {
        require(charCode > end)

        val distance = charCode - end
        if (mapping == this.mapping
            && length < MAX_LENGTH
            && distance <= MAX_DISTANCE
            && (distance == this.distance || length == 1)
        ) {
            // if length is equal to 1, distance can be changed
            this.distance = distance
            end = charCode
            return true
        }
        return false
    }

    override fun toString(): String {
        return "EqualDistanceMappingPattern{" +
                "start=" + start.toHexIntLiteral() +
                ", end=" + end.toHexIntLiteral() +
                ", length=" + length +
                ", distance=" + distance +
                ", count=" + ((length + distance - 1) / distance) +
                ", mapping=" + mapping +
                "}"
    }

    companion object {
        private const val MAX_DISTANCE = 15
        private const val MAX_LENGTH = 255

        fun from(charCode: Int, categoryCode: String, mapping: Int): EqualDistanceMappingPattern {
            return EqualDistanceMappingPattern(charCode, categoryCode, mapping)
        }
    }
}