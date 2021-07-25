/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.builders

import generators.unicode.UnicodeDataLine
import generators.unicode.mappings.oneToOne.patterns.EqualDistanceMappingPattern
import generators.unicode.mappings.oneToOne.patterns.MappingPattern
import generators.unicode.hexToInt

/**
 * The base class of character mappings builders.
 */
internal abstract class MappingsBuilder {
    private val patterns = mutableListOf<MappingPattern>()

    /**
     * Appends a line from the UnicodeData.txt file.
     */
    fun append(line: UnicodeDataLine) {
        val charCode = line.char.hexToInt()
        val equivalent = mappingEquivalent(line)?.hexToInt() ?: return
        val mapping = equivalent - charCode

        check((charCode > Char.MAX_VALUE.code) == (equivalent > Char.MAX_VALUE.code)) { "Handle when equivalent mapping is out of BMP." }

        if (patterns.isEmpty()) {
            patterns.add(createPattern(charCode, line.categoryCode, mapping))
            return
        }

        val lastPattern = patterns.last()

        if (!lastPattern.append(charCode, line.categoryCode, mapping)) {
            val newLastPattern = evolveLastPattern(lastPattern, charCode, line.categoryCode, mapping)
            if (newLastPattern != null) {
                patterns[patterns.lastIndex] = newLastPattern
            } else {
                patterns.add(createPattern(charCode, line.categoryCode, mapping))
            }
        }
    }

    /**
     * Returns the resulting mapping patterns.
     */
    fun build(): List<MappingPattern> {
//        println(patterns.joinToString(separator = "\n"))
//        println("${this.javaClass} # ${patterns.size}")
        return patterns
    }

    /**
     * Returns the mapping equivalent this builder is responsible for.
     */
    abstract fun mappingEquivalent(line: UnicodeDataLine): String?

    /**
     * Appends the [charCode] with the specified [categoryCode] and [mapping] to the [lastPattern] and returns the resulting pattern,
     * or returns `null` if the [charCode] can't be appended to the [lastPattern].
     * The [lastPattern] can be transformed to another pattern type to accommodate the [charCode].
     */
    protected open fun evolveLastPattern(lastPattern: MappingPattern, charCode: Int, categoryCode: String, mapping: Int): MappingPattern? {
        return null
    }

    private fun createPattern(charCode: Int, categoryCode: String, mapping: Int): MappingPattern {
        return EqualDistanceMappingPattern.from(charCode, categoryCode, mapping)
    }
}
