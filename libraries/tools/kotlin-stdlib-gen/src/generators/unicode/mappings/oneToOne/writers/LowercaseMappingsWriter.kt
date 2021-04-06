/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.mappings.oneToOne.writers

import generators.unicode.mappings.oneToOne.patterns.EqualDistanceMappingPattern
import generators.unicode.mappings.oneToOne.patterns.length
import generators.unicode.mappings.oneToOne.patterns.MappingPattern
import generators.unicode.ranges.RangesWritingStrategy
import generators.unicode.writeIntArray
import java.io.FileWriter

internal class LowercaseMappingsWriter(private val strategy: RangesWritingStrategy) : MappingsWriter {
    override fun write(mappings: List<MappingPattern>, writer: FileWriter) {
        @Suppress("UNCHECKED_CAST")
        val distanceMappings = mappings as List<EqualDistanceMappingPattern>

        val start = distanceMappings.map { it.start }
        val length = distanceMappings.map { (it.mapping shl 12) or (it.distance shl 8) or it.length }

        strategy.beforeWritingRanges(writer)
        writer.writeIntArray("rangeStart", start, strategy)
        writer.appendLine()
        writer.writeIntArray("rangeLength", length, strategy)
        strategy.afterWritingRanges(writer)
        writer.appendLine()
        writer.appendLine(lowercaseCodePoint())
        writer.appendLine()
        writer.appendLine(lowercaseCharImpl())
    }

    private fun lowercaseCodePoint(): String = """
        internal fun Int.lowercaseCodePoint(): Int {
            if (this in 0x41..0x5a) {
                return this + 32
            }
            if (this < 0x80) {
                return this
            }
            val index = binarySearchRange(rangeStart, this)
            return equalDistanceMapping(this, rangeStart[index], rangeLength[index])
        }
    """.trimIndent()

    private fun lowercaseCharImpl(): String = """
        internal fun Char.lowercaseCharImpl(): Char {
            return code.lowercaseCodePoint().toChar()
        }
    """.trimIndent()
}