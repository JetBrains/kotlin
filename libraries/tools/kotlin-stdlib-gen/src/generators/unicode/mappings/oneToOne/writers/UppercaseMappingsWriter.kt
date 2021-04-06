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

internal class UppercaseMappingsWriter(private val strategy: RangesWritingStrategy) : MappingsWriter {
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
        writer.appendLine(equalDistanceMapping())
        writer.appendLine()
        writer.appendLine(uppercaseCodePoint())
        writer.appendLine()
        writer.appendLine(uppercaseCharImpl())
    }

    private fun equalDistanceMapping(): String = """
        internal fun equalDistanceMapping(code: Int, start: Int, pattern: Int): Int {
            val diff = code - start

            val length = pattern and 0xff
            if (diff >= length) {
                return code
            }

            val distance = (pattern shr 8) and 0xf
            if (diff % distance != 0) {
                return code
            }

            val mapping = pattern shr 12
            return code + mapping
        }
    """.trimIndent()

    private fun uppercaseCodePoint(): String = """
        internal fun Int.uppercaseCodePoint(): Int {
            if (this in 0x61..0x7a) {
                return this - 32
            }
            if (this < 0x80) {
                return this
            }
            val index = binarySearchRange(rangeStart, this)
            return equalDistanceMapping(this, rangeStart[index], rangeLength[index])
        }
    """.trimIndent()

    private fun uppercaseCharImpl(): String = """
        internal fun Char.uppercaseCharImpl(): Char {
            return code.uppercaseCodePoint().toChar()
        }
    """.trimIndent()
}