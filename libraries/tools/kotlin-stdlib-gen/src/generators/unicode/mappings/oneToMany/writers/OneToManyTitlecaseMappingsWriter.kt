/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToMany.writers

import generators.unicode.ranges.RangesWritingStrategy
import generators.unicode.writeMappings
import java.io.FileWriter

internal class OneToManyTitlecaseMappingsWriter(private val strategy: RangesWritingStrategy) : OneToManyMappingsWriter {
    override fun write(mappings: Map<Int, List<String>>, writer: FileWriter) {
        strategy.beforeWritingRanges(writer)
        writer.writeMappings(mappings, strategy)
        strategy.afterWritingRanges(writer)
        writer.appendLine()
        writer.appendLine(titlecaseImpl())
    }

    private fun titlecaseImpl(): String = """
        internal fun Char.titlecaseImpl(): String {
            val code = this.toInt()
            val index = binarySearchRange(keys, code)
            if (index >= 0 && keys[index] == code) {
                return values[index]
            }
            return titlecaseCharImpl().toString()
        }
    """.trimIndent()
}
