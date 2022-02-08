/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.writers

import generators.unicode.mappings.oneToOne.patterns.EqualDistanceMappingPattern
import generators.unicode.mappings.oneToOne.patterns.LuLtLlMappingPattern
import generators.unicode.mappings.oneToOne.patterns.MappingPattern
import generators.unicode.toHexIntLiteral
import java.io.FileWriter

internal class TitlecaseMappingsWriter : MappingsWriter {

    override fun write(mappings: List<MappingPattern>, writer: FileWriter) {
        val LuLtLlMappings = mappings.filterIsInstance<LuLtLlMappingPattern>()
        val zeroMappings = mappings.filterIsInstance<EqualDistanceMappingPattern>().filter { it.distance == 1 && it.mapping == 0 }

        check(LuLtLlMappings.size + zeroMappings.size == mappings.size) { "Handle new types of titlecase mapping." }
        check(LuLtLlMappings.all { it.start % 3 == 2 }) { "Handle when code of the Lt char is not multiple of 3." }

        writer.append(
            """
            internal fun Char.titlecaseCharImpl(): Char {
                val code = this.code
                // Letters repeating <Lu, Lt, Ll> sequence and code of the Lt is a multiple of 3, e.g. <Ǆ, ǅ, ǆ>
                if (${rangeChecks(LuLtLlMappings, "code")}) {
                    return (3 * ((code + 1) / 3)).toChar()
                }
                // Lower case letters whose title case mapping equivalent is equal to the original letter
                if (${rangeChecks(zeroMappings, "code")}) {
                    return this
                }
                return uppercaseChar()
            }
            """.trimIndent()
        )
    }

    private fun rangeChecks(mappings: List<MappingPattern>, code: String): String {
        return mappings.joinToString(separator = " || ") { "$code in ${it.start.toHexIntLiteral()}..${it.end.toHexIntLiteral()}" }
    }
}