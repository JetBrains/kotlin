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

internal class CharCategoryTestGenerator(private val outputFile: File) {
    private var arrayIndex = 0
    private var arraySize = 0
    private var writer: FileWriter? = null

    init {
        outputFile.parentFile.mkdirs()
    }

    fun appendLine(line: UnicodeDataLine) {
        if (arraySize == 0) {
            writer?.appendLine(")")
            writer?.close()

            generateUnicodeDataHeader(arrayIndex)
        }

        val isStart = line.name.endsWith(", First>")

        writer?.appendLine("    CharProperties(char = '\\u${line.char}', isStartOfARange = $isStart, categoryCode = \"${line.categoryCode}\"),")

        arraySize++
        if (arraySize == 2048) {
            arraySize = 0
            arrayIndex++
        }
    }

    private val otherLowercaseRanges = mutableListOf<PropertyLine>()
    private val otherUppercaseRanges = mutableListOf<PropertyLine>()

    fun appendPropertyLine(line: PropertyLine) {
        when (line.property) {
            "Other_Lowercase" -> otherLowercaseRanges.add(line)
            "Other_Uppercase" -> otherUppercaseRanges.add(line)
        }
    }

    fun generate() {
        writer?.appendLine(")")
        writer?.close()

        generateFlattenUnicodeData()
        generateCharProperties()
        generateCharCategoryTest()
    }

    private fun generateFlattenUnicodeData() {
        val file = outputFile.resolveSibling("_UnicodeDataFlatten.kt")
        generateFileHeader(file)

        writer?.appendLine("internal val unicodeData = arrayOf<Array<CharProperties>>(")
        for (index in 0..arrayIndex) {
            writer?.appendLine("    unicodeData$index,")
        }
        writer?.appendLine(").flatten()")

        writer?.close()
    }

    private fun generateCharProperties() {
        val file = outputFile.resolveSibling("_CharProperties.kt")
        generateFileHeader(file)

        writer?.appendLine("data class CharProperties(val char: Char, val isStartOfARange: Boolean, val categoryCode: String)")
        writer?.close()
    }

    private fun generateCharCategoryTest() {
        generateFileHeader(outputFile)

        writer?.appendLine(
            """
import kotlin.test.*

class CharCategoryTest {
    @Test
    fun category() {
        val charProperties = hashMapOf<Char, CharProperties>()

        for (properties in unicodeData) {
            charProperties[properties.char] = properties
        }

        var properties: CharProperties? = null

        for (char in Char.MIN_VALUE..Char.MAX_VALUE) {
            if (charProperties.containsKey(char)) {
                properties = charProperties.getValue(char)
            } else if (properties?.isStartOfARange != true) {
                properties = null
            }

            val charCode = char.code.toString(radix = 16).padStart(length = 4, padChar = '0')
            val expectedCategoryCode = properties?.categoryCode ?: CharCategory.UNASSIGNED.code

            fun <T> test(expected: T, actual: T, name: String) {
                assertEquals(expected, actual, "Char:[${"$"}char] with code:[${"$"}charCode] in Unicode has ${"$"}name = ${"$"}expected, but in Kotlin ${"$"}name = ${"$"}actual")
            }

            test(expectedCategoryCode, char.category.code, "category")

            val expectedIsDigit = isDigit(expectedCategoryCode)
            test(expectedIsDigit, char.isDigit(), "isDigit()")
            
            val expectedIsLetter = isLetter(expectedCategoryCode)
            test(expectedIsLetter, char.isLetter(), "isLetter()")
            
            val expectedIsLetterOrDigit = expectedIsLetter || expectedIsDigit
            test(expectedIsLetterOrDigit, char.isLetterOrDigit(), "isLetterOrDigit()")
            
            val expectedIsLowerCase = isLowerCase(char, expectedCategoryCode)
            test(expectedIsLowerCase, char.isLowerCase(), "isLowerCase()")

            val expectedIsUpperCase = isUpperCase(char, expectedCategoryCode)
            test(expectedIsUpperCase, char.isUpperCase(), "isUpperCase()")

            val expectedIsWhitespace = isWhitespace(char, expectedCategoryCode)
            test(expectedIsWhitespace, char.isWhitespace(), "isWhitespace()")
        }
    }

    private fun isDigit(categoryCode: String): Boolean {
        return categoryCode == CharCategory.DECIMAL_DIGIT_NUMBER.code
    }

    private fun isLetter(categoryCode: String): Boolean {
        return categoryCode in listOf(
            CharCategory.UPPERCASE_LETTER,
            CharCategory.LOWERCASE_LETTER,
            CharCategory.TITLECASE_LETTER,
            CharCategory.MODIFIER_LETTER,
            CharCategory.OTHER_LETTER
        ).map { it.code }
    }

    private val otherLowerChars = listOf<IntRange>(
        ${otherLowercaseRanges.joinToString { it.hexIntRangeLiteral() }}
    ).flatten().toHashSet()

    private fun isLowerCase(char: Char, categoryCode: String): Boolean {
        return categoryCode == CharCategory.LOWERCASE_LETTER.code || otherLowerChars.contains(char.code)
    }

    private val otherUpperChars = listOf<IntRange>(
        ${otherUppercaseRanges.joinToString { it.hexIntRangeLiteral() }}
    ).flatten().toHashSet()

    private fun isUpperCase(char: Char, categoryCode: String): Boolean {
        return categoryCode == CharCategory.UPPERCASE_LETTER.code || otherUpperChars.contains(char.code)
    }

    private fun isWhitespace(char: Char, categoryCode: String): Boolean {
        return categoryCode in listOf(
            CharCategory.SPACE_SEPARATOR.code,
            CharCategory.LINE_SEPARATOR.code,
            CharCategory.PARAGRAPH_SEPARATOR.code
        ) || char in '\u0009'..'\u000D' || char in '\u001C'..'\u001F'
    }
}
            """.trimIndent()
        )

        writer?.close()
    }

    private fun generateUnicodeDataHeader(arrayIndex: Int) {
        val file = outputFile.resolveSibling("_UnicodeData$arrayIndex.kt")
        generateFileHeader(file)

        writer?.appendLine("internal val unicodeData$arrayIndex = arrayOf<CharProperties>(")
    }

    private fun generateFileHeader(file: File) {
        writer = FileWriter(file)
        writer?.writeHeader(file, "test.text.unicodeData")
        writer?.appendLine()
    }
}
