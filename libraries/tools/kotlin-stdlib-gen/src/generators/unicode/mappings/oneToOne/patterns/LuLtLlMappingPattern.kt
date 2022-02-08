/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.patterns

import generators.unicode.toHexIntLiteral

/**
 * A range of consequent chars that repeat <Lu, Lt, Ll> sequence of `categoryCode` and <1, 0, -1> sequence of `mapping`.
 *
 * @param charCode the start of this range
 * @param categoryCode the category code of the char with the specified [charCode]
 * @param mapping the difference between the [charCode] and the char it converts to.
 */
internal class LuLtLlMappingPattern private constructor(
    charCode: Int,
    categoryCode: String,
    mapping: Int
) : MappingPattern {
    override val start = charCode
    override var end = charCode
        private set
    val distance = 1

    init {
        require(categoryCode == "Lu" && mapping == 1)
    }

    override fun append(charCode: Int, categoryCode: String, mapping: Int): Boolean {
        require(charCode > end)

        val distance = charCode - end
        val modThree = (charCode - start) % 3
        val expectedMapping = expectedMapping(categoryCode)
        val expectedModThree = expectedModThree(categoryCode)
        if (distance == this.distance && mapping == expectedMapping && modThree == expectedModThree) {
            end = charCode
            return true
        }
        return false
    }

    private fun expectedModThree(categoryCode: String): Int = when (categoryCode) {
        "Lu" -> 0
        "Lt" -> 1
        "Ll" -> 2
        else -> error("Unexpected category: $categoryCode")
    }

    private fun expectedMapping(categoryCode: String): Int = when (categoryCode) {
        "Lu" -> 1
        "Lt" -> 0
        "Ll" -> -1
        else -> error("Unexpected category: $categoryCode")
    }

    override fun toString(): String {
        return "LuLtLlMappingPattern{" +
                "start=" + start.toHexIntLiteral() +
                ", end=" + end.toHexIntLiteral() +
                ", length=" + length +
                ", distance=" + distance +
                ", count=" + ((length + distance - 1) / distance) +
                "}"
    }

    companion object {
        fun from(pattern: MappingPattern, charCode: Int, categoryCode: String, mapping: Int): LuLtLlMappingPattern? {
            if (pattern is EqualDistanceMappingPattern
                && pattern.length == 1
                && pattern.categoryCode == "Lu"
                && pattern.mapping == 1
            ) {
                val newPattern = LuLtLlMappingPattern(pattern.start, pattern.categoryCode, pattern.mapping)
                if (newPattern.append(charCode, categoryCode, mapping)) {
                    return newPattern
                }
            }
            return null
        }
    }
}