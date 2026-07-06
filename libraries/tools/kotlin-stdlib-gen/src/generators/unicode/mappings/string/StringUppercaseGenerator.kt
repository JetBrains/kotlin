/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.string

import generators.unicode.SpecialCasingLine
import generators.unicode.UnicodeDataLine
import generators.unicode.writeHeader
import templates.KotlinTarget
import java.io.File
import java.io.FileWriter

internal class StringUppercaseGenerator(
    private val outputFile: File,
    unicodeDataLines: List<UnicodeDataLine>,
    private val target: KotlinTarget
) : StringCasingGenerator(unicodeDataLines) {

    override fun SpecialCasingLine.mapping(): List<String> = uppercaseMapping

    override fun UnicodeDataLine.mapping(): String = uppercaseMapping

    fun generate() {
        check(contextDependentMappings.isEmpty()) {
            "The locale-agnostic conditional mappings $contextDependentMappings are not handled."
        }
//
//        FileWriter(outputFile).use { writer ->
//            writer.writeHeader(outputFile, "kotlin.text")
//            writer.appendLine()
//        }
    }

}
