/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.builders

import generators.unicode.UnicodeDataLine
import generators.unicode.mappings.oneToOne.patterns.LuLtLlMappingPattern
import generators.unicode.mappings.oneToOne.patterns.MappingPattern

internal class TitlecaseMappingsBuilder : MappingsBuilder() {

    override fun mappingEquivalent(line: UnicodeDataLine): String? {
        if (line.titlecaseMapping == line.uppercaseMapping) return null
        check(line.titlecaseMapping.isNotEmpty()) { "UnicodeData.txt format has changed!" }
        return line.titlecaseMapping
    }

    override fun evolveLastPattern(lastPattern: MappingPattern, charCode: Int, categoryCode: String, mapping: Int): MappingPattern? {
        return LuLtLlMappingPattern.from(lastPattern, charCode, categoryCode, mapping)
    }
}
