/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges

import generators.unicode.PropertyLine
import generators.unicode.hexToInt
import generators.unicode.rangeCheck
import generators.unicode.writeHeader
import templates.KotlinTarget
import java.io.File
import java.io.FileWriter

internal class OtherUppercaseRangesGenerator(
    private val outputFile: File,
    private val target: KotlinTarget
) {
    private val otherUpperRanges = mutableListOf<IntRange>()

    fun appendLine(line: PropertyLine) {
        // In Native the Other_Uppercase code points are also used to perform String.lowercase()
        if (target != KotlinTarget.Native && line.rangeStart.hexToInt() > 0xFFFF) return

        if (line.property == "Other_Uppercase") {
            otherUpperRanges.add(line.intRange())
        }
    }

    fun generate() {
        FileWriter(outputFile).use { writer ->
            writer.writeHeader(outputFile, "kotlin.text")
            writer.appendLine()
            writer.appendLine(isOtherUppercaseImpl())
        }
    }

    fun isOtherUppercaseImpl(): String {
        val indent = "    ".repeat(5)
        val builder = StringBuilder()

        for (i in otherUpperRanges.indices) {
            if (i != 0) {
                builder.appendLine().append(indent).append("|| ")
            }
            builder.append(otherUpperRanges[i].rangeCheck("this", indent))
        }

        return """
        internal fun Int.isOtherUppercase(): Boolean {
            return $builder
        }
        """.trimIndent()
    }
}