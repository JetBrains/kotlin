/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.builders

import generators.unicode.UnicodeDataLine

internal class UppercaseMappingsBuilder : MappingsBuilder() {
    override fun mappingEquivalent(line: UnicodeDataLine): String? {
        if (line.uppercaseMapping.isEmpty()) return null
        check(line.char != line.uppercaseMapping) { "UnicodeData.txt format has changed!" }
        return line.uppercaseMapping
    }
}
