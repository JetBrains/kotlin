/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToMany.writers

import generators.unicode.ranges.RangesWritingStrategy
import generators.unicode.toHexCharLiteral
import generators.unicode.hexCharsToStringLiteral
import java.io.FileWriter

internal class OneToManyLowercaseMappingsWriter(private val strategy: RangesWritingStrategy) : OneToManyMappingsWriter {
    override fun write(mappings: Map<Int, List<String>>, writer: FileWriter) {
        check(mappings.size == 1) { "Number of multi-char lowercase mappings has changed." }

        val (key, value) = mappings.entries.single()
        val char = key.toHexCharLiteral()
        val result = value.hexCharsToStringLiteral()

        writer.appendLine(lowercaseImpl(char, result))
    }

    private fun lowercaseImpl(char: String, oneToManyResult: String): String = """
        internal fun Char.lowercaseImpl(): String {
            if (this == $char) {
                return $oneToManyResult
            }
            return lowercaseCharImpl().toString()
        }
    """.trimIndent()
}