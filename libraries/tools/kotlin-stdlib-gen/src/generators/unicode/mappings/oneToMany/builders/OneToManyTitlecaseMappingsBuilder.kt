/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToMany.builders

import generators.unicode.SpecialCasingLine
import generators.unicode.UnicodeDataLine
import generators.unicode.hexToInt

internal class OneToManyTitlecaseMappingsBuilder(bmpUnicodeDataLines: List<UnicodeDataLine>) : OneToManyMappingsBuilder(bmpUnicodeDataLines) {
    private fun lower(char: Int): Int {
        return bmpUnicodeDataLines[char]?.lowercaseMapping?.takeIf { it.isNotEmpty() }?.hexToInt() ?: char
    }

    override fun SpecialCasingLine.mapping(): List<String>? {
        val title = titlecaseMapping.map { it.hexToInt().toChar() }.joinToString(separator = "")
        val upperFirst = uppercaseMapping[0].hexToInt().toChar()

        // The version of Unicode in JVM executing this code is likely different than the version of the Unicode being parsed.
        // Thus use the Unicode data being parsed to convert chars to lowercase.
        val lowercasedTail = uppercaseMapping.drop(1).map { lower(it.hexToInt()).toChar() }.joinToString(separator = "")

        // Skip titlecase mappings that can be derived by lowercasing tail characters
        if (title == upperFirst + lowercasedTail) {
            return null
        }

        return titlecaseMapping
    }

    override fun UnicodeDataLine.mapping(): String = titlecaseMapping
}
