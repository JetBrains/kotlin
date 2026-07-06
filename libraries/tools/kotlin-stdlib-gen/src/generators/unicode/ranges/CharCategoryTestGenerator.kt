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

        writer?.appendLine("    CharProperties(code = 0x${line.char}, isStartOfARange = $isStart, categoryCode = \"${line.categoryCode}\"),")

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

        writer?.appendLine("data class CharProperties(val code: Int, val isStartOfARange: Boolean, val categoryCode: String)")
        writer?.close()
    }

    private fun generateCharCategoryTest() {
        generateFileHeader(outputFile)

        writer?.appendLine(
            $$"""
import kotlin.text.codepoints.*
import kotlin.test.*
import test.TestPlatform
import test.current

@OptIn(ExperimentalCodePointApi::class, ExperimentalKotlinTestApi::class)
class CharCategoryTest {
    @Test
    fun category() {
        val charProperties = hashMapOf<Int, CharProperties>()

        for (properties in unicodeData) {
            charProperties[properties.code] = properties
        }

        var properties: CharProperties? = null

        for (codePoint in CodePoint.MIN_VALUE..CodePoint.MAX_VALUE) {
            if (charProperties.containsKey(codePoint.code)) {
                properties = charProperties.getValue(codePoint.code)
            } else if (properties?.isStartOfARange != true) {
                properties = null
            }

            val expectedCategoryCode = properties?.categoryCode ?: CharCategory.UNASSIGNED.code
            val expectedIsDigit = isDigit(expectedCategoryCode)
            val expectedIsLetter = isLetter(expectedCategoryCode)
            val expectedIsLetterOrDigit = expectedIsLetter || expectedIsDigit
            val expectedIsLowerCase = isLowerCase(codePoint.code, expectedCategoryCode)
            val expectedIsUpperCase = isUpperCase(codePoint.code, expectedCategoryCode)
            val expectedIsWhitespace = isWhitespace(codePoint.code, expectedCategoryCode)

            fun <T> test(expected: T, actual: T, property: String) {
                if (expected != actual) {
                    val charCode = "U+" + codePoint.code.toString(radix = 16).padStart(length = 4, padChar = '0')
                    if (TestPlatform.current == TestPlatform.Jvm) {
                        // it's expected that on JVM different Unicode version can have some characters unassigned or assigned
                        // just report it
                        println("Character $charCode [$codePoint] $property differs: expected $expected, actual $actual")
                    } else {
                        assertEquals(expected, actual) { "Character $charCode [$codePoint] $property differs" }
                    }
                }
            }

            test(expectedCategoryCode, codePoint.category.code, "CodePoint.category")
            if (expectedCategoryCode != codePoint.category.code &&
                (expectedCategoryCode == CharCategory.UNASSIGNED.code || codePoint.category == CharCategory.UNASSIGNED) &&
                TestPlatform.current == TestPlatform.Jvm) continue

            test(expectedIsDigit, codePoint.isDigit(), "CodePoint.isDigit()")
            test(expectedIsLetter, codePoint.isLetter(), "CodePoint.isLetter()")
            test(expectedIsLetterOrDigit, codePoint.isLetterOrDigit(), "CodePoint.isLetterOrDigit()")
            test(expectedIsLowerCase, codePoint.isLowerCase(), "CodePoint.isLowerCase()")
            test(expectedIsUpperCase, codePoint.isUpperCase(), "CodePoint.isUpperCase()")
            test(expectedIsWhitespace, codePoint.isWhitespace(), "CodePoint.isWhitespace()")
            if (codePoint.isBasic) {
                val char = codePoint.toSingleChar()
                test(expectedCategoryCode, char.category.code, "Char.category")
                test(expectedIsDigit, char.isDigit(), "Char.isDigit()")
                test(expectedIsLetter, char.isLetter(), "Char.isLetter()")
                test(expectedIsLetterOrDigit, char.isLetterOrDigit(), "Char.isLetterOrDigit()")
                test(expectedIsLowerCase, char.isLowerCase(), "Char.isLowerCase()")
                test(expectedIsUpperCase, char.isUpperCase(), "Char.isUpperCase()")
                test(expectedIsWhitespace, char.isWhitespace(), "Char.isWhitespace()")
            }
        }
    }

    private fun isDigit(categoryCode: String): Boolean {
        return categoryCode == CharCategory.DECIMAL_DIGIT_NUMBER.code
    }

    private val letterCodes = listOf(
            CharCategory.UPPERCASE_LETTER,
            CharCategory.LOWERCASE_LETTER,
            CharCategory.TITLECASE_LETTER,
            CharCategory.MODIFIER_LETTER,
            CharCategory.OTHER_LETTER
        ).map { it.code }.toSet()
        
    private fun isLetter(categoryCode: String): Boolean {
        return categoryCode in letterCodes
    }

    private val otherLowerChars = listOf<IntRange>(
        $${otherLowercaseRanges.joinToString { it.hexIntRangeLiteral() }}
    ).flatten().toHashSet()

    private fun isLowerCase(code: Int, categoryCode: String): Boolean {
        return categoryCode == CharCategory.LOWERCASE_LETTER.code || otherLowerChars.contains(code)
    }

    private val otherUpperChars = listOf<IntRange>(
        $${otherUppercaseRanges.joinToString { it.hexIntRangeLiteral() }}
    ).flatten().toHashSet()

    private fun isUpperCase(code: Int, categoryCode: String): Boolean {
        return categoryCode == CharCategory.UPPERCASE_LETTER.code || otherUpperChars.contains(code)
    }

    private val whitespaceCodes = setOf(
            CharCategory.SPACE_SEPARATOR.code,
            CharCategory.LINE_SEPARATOR.code,
            CharCategory.PARAGRAPH_SEPARATOR.code
        )
        
    private fun isWhitespace(code: Int, categoryCode: String): Boolean {
        return categoryCode in whitespaceCodes || code in 0x0009..0x000D || code in 0x001C..0x001F
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
