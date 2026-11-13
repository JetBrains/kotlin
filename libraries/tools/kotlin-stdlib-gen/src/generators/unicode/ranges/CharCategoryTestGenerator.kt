/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges

import generators.unicode.PropertyLine
import generators.unicode.UnicodeDataLine
import generators.unicode.writeHeader
import java.io.File
import java.io.FileWriter

internal class CharCategoryTestGenerator(
    private val outputFile: File,
    private val unicodeDataLines: List<UnicodeDataLine>,
    propListLines: List<PropertyLine>
) {

    private val otherLowercaseRanges = propListLines.filter { it.property == "Other_Lowercase" }
    private val otherUppercaseRanges = propListLines.filter { it.property == "Other_Uppercase" }

    init {
        outputFile.parentFile.mkdirs()
    }

    fun generate() {
        generateUnicodeDataFiles(unicodeDataLines, "full/_UnicodeDataFull", "unicodeDataFull")
        generateCharCategoryFullTest()

        val reducedRanges = [
            0x0000..<0x0400,
            0x1680..0x1680, 0x2000..0x205F, 0x3000..0x3000, // whitespaces
            0xFFD0..0xFFFF,
            0x11730..0x11739, // supplementary digits
            0x1E900..0x1E943, // supplementary upper/lower case letters
            // Several Unicode 13.0.0 ranges
            0x10E80..0x10EBF,
            0x1FB00..0x1FBFF,
        ]
        val reducedCodes = (
            reducedRanges + otherLowercaseRanges.map { it.intRange() } + otherUppercaseRanges.map { it.intRange() }
        ).flatMapTo(HashSet(), { it })

        generateUnicodeDataFiles(unicodeDataLines.filter { it.char.hexToInt() in reducedCodes }, "_UnicodeData", "unicodeDataReduced")
        generateCharCategoryTest()
    }

    private fun generateUnicodeDataFiles(lines: List<UnicodeDataLine>, fileBaseName: String, propertyBaseName: String) {
        val parts = lines.chunked(2048)
        for ([index, part] in parts.withIndex()) {
            val file = outputFile.resolveSibling("$fileBaseName$index.kt")
            file.parentFile.mkdirs()
            generateFileHeader(file).use { writer ->
                writer.appendLine("internal val $propertyBaseName$index = arrayOf<CharProperties>(")
                for (line in part) {
                    val isStart = line.name.endsWith(", First>")
                    writer.appendLine("    CharProperties(code = 0x${line.char}, categoryCode = \"${line.categoryCode}\"${if (isStart) ", isStartOfARange = true" else ""}),")
                }
                writer.appendLine(")")
            }
        }

        val file = outputFile.resolveSibling("${fileBaseName}Flatten.kt")
        generateFileHeader(file).use { writer ->
            writer.appendLine("internal val $propertyBaseName = arrayOf<Array<CharProperties>>(")
            for (index in parts.indices) {
                writer.appendLine("    $propertyBaseName$index,")
            }
            writer.appendLine(").flatten()")
        }
    }

    private fun generateCharCategoryTest() {
        generateFileHeader(outputFile).use { writer ->
            writer.appendLine(
                $$"""
    class CharCategoryTest : CharCategoryTestBase(
        isFull = false,
        charProperties = unicodeDataReduced.associateBy { it.code },
    )

    internal val _otherLowerChars = listOf<IntRange>(
        $${otherLowercaseRanges.joinToString { it.hexIntRangeLiteral() }}
    ).flatMapTo(HashSet(), { it })

    internal val _otherUpperChars = listOf<IntRange>(
        $${otherUppercaseRanges.joinToString { it.hexIntRangeLiteral() }}
    ).flatMapTo(HashSet(), { it })
            """.trimIndent()
            )
        }
    }

    private fun generateCharCategoryFullTest() {
        generateFileHeader(outputFile.resolveSibling("full/_CharCategoryFullTest.kt")).use { writer ->
            writer.appendLine("""
                class CharCategoryFullTest : CharCategoryTestBase(
                    isFull = true,
                    charProperties = unicodeDataFull.associateBy { it.code },
                )
            """.trimIndent())
        }
    }

    private fun generateFileHeader(file: File): FileWriter {
        val writer = FileWriter(file)
        writer.writeHeader(file, "test.text.unicodeData")
        writer.appendLine()
        return writer
    }
}
