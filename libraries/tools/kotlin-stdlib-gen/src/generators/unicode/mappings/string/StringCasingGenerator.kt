/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.string

import generators.unicode.SpecialCasingLine
import generators.unicode.UnicodeDataLine
import generators.unicode.hexToInt

internal abstract class StringCasingGenerator(
    unicodeDataLines: List<UnicodeDataLine>
) {
    private val unicodeDataLines = unicodeDataLines.associateBy { it.char.hexToInt() }
    protected val contextDependentMappings = mutableListOf<SpecialCasingLine>()

    fun appendSpecialCasingLine(line: SpecialCasingLine) {
        if (line.conditionList.isEmpty()) return

        val isLocaleAgnosticCondition = line.conditionList.all { it.length > 2 }

        val unicodeLine = unicodeDataLines[line.char.hexToInt()]
        val unicodeDataMapping = unicodeLine?.mapping()?.takeIf { it.isNotEmpty() } ?: line.char

        if (isLocaleAgnosticCondition && line.mapping() != listOf(unicodeDataMapping)) {
            contextDependentMappings.add(line)
        }
    }

    abstract fun SpecialCasingLine.mapping(): List<String>
    abstract fun UnicodeDataLine.mapping(): String
}