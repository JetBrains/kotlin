/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToMany.builders

import generators.unicode.SpecialCasingLine
import generators.unicode.UnicodeDataLine
import generators.unicode.hexToInt
import java.util.TreeMap

internal abstract class OneToManyMappingsBuilder(bmpUnicodeDataLines: List<UnicodeDataLine>) {
    protected val bmpUnicodeDataLines = bmpUnicodeDataLines.associateBy { it.char.hexToInt() }
    private val mappings = TreeMap<Int, List<String>>()

    fun append(line: SpecialCasingLine) {
        if (line.conditionList.isNotEmpty()) return

        val charCode = line.char.hexToInt()

        check(charCode <= Char.MAX_VALUE.code) { "Handle special casing for the supplementary code point: $line" }

        val mapping = mapping(charCode, line) ?: return

        mappings[charCode] = mapping
    }

    fun build(): Map<Int, List<String>> {
//        println(mappings)
//        println("${this.javaClass} # ${mappings.size}")
        return mappings
    }

    private fun mapping(charCode: Int, line: SpecialCasingLine): List<String>? {
        val mapping = line.mapping() ?: return null

        check(mapping.isNotEmpty() && mapping.all { it.isNotEmpty() })

        if (mapping.size == 1) {
            val specialCasingMapping = mapping.first()

            val unicodeLine = bmpUnicodeDataLines[charCode]
            val unicodeDataMapping = unicodeLine?.mapping()?.takeIf { it.isNotEmpty() } ?: line.char

            check(unicodeDataMapping == specialCasingMapping) {
                "UnicodeData.txt and SpecialCasing.txt files have different single char case conversion"
            }

            return null
        }

        return mapping
    }

    abstract fun SpecialCasingLine.mapping(): List<String>?
    abstract fun UnicodeDataLine.mapping(): String
}
