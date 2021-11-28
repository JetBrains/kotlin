/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges

import generators.unicode.PropertyLine
import generators.unicode.hexToInt
import generators.unicode.writeHeader
import generators.unicode.writeIntArray
import templates.KotlinTarget
import java.io.File
import java.io.FileWriter

internal class OtherLowercaseRangesGenerator(
    private val outputFile: File,
    private val target: KotlinTarget
) {
    private val otherLowerRanges = mutableListOf<IntRange>()

    fun appendLine(line: PropertyLine) {
        // In Native the Other_Lowercase code points are also used to perform String.lowercase()
        if (target != KotlinTarget.Native && line.rangeStart.hexToInt() > 0xFFFF) return

        if (line.property == "Other_Lowercase") {
            otherLowerRanges.add(line.intRange())
        }
    }

    fun generate() {
        val strategy = RangesWritingStrategy.of(target, "OtherLowercase")

        FileWriter(outputFile).use { writer ->
            writer.writeHeader(outputFile, "kotlin.text")
            writer.appendLine()
            strategy.beforeWritingRanges(writer)
            writer.writeIntArray("otherLowerStart", otherLowerRanges.map { it.first }, strategy)
            writer.writeIntArray("otherLowerLength", otherLowerRanges.map { it.last - it.first + 1 }, strategy, useHex = false)
            strategy.afterWritingRanges(writer)
            writer.appendLine()
            writer.appendLine(isOtherLowercaseImpl(strategy))
        }
    }

    fun isOtherLowercaseImpl(strategy: RangesWritingStrategy) = """
        internal fun Int.isOtherLowercase(): Boolean {
            val index = binarySearchRange(${strategy.rangeRef("otherLowerStart")}, this)
            return index >= 0 && this < ${strategy.rangeRef("otherLowerStart")}[index] + ${strategy.rangeRef("otherLowerLength")}[index]
        }
    """.trimIndent()
}