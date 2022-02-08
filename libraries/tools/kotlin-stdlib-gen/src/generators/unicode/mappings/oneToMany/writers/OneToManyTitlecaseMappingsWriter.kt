/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToMany.writers

import generators.unicode.toHexCharLiteral
import java.io.FileWriter

internal class OneToManyTitlecaseMappingsWriter : OneToManyMappingsWriter {
    override fun write(mappings: Map<Int, List<String>>, writer: FileWriter) {
        // We have decided to ignore GREEK EXTENDED block due to their rare usage.
        // It also leads to decreased js code size and simplified implementation.
        val nonGreekExtended = mappings.filterNot { it.key in 0x1f00..0x1fff }

        check(nonGreekExtended.size == 1)
        val n = nonGreekExtended.keys.single()
        check(n == 0x0149) // LATIN SMALL LETTER N PRECEDED BY APOSTROPHE

        // one-to-many titlecase equivalent of the char is "\u02BC\u004E", the same as the uppercase equivalent
        writer.appendLine(titlecaseImpl(n.toHexCharLiteral()))
    }

    private fun titlecaseImpl(apostropheN: String): String = """
        internal fun Char.titlecaseImpl(): String {
            val uppercase = uppercase()
            if (uppercase.length > 1) {
                return if (this == $apostropheN) uppercase else uppercase[0] + uppercase.substring(1).lowercase()
            }
            return titlecaseChar().toString()
        }
    """.trimIndent()
}
