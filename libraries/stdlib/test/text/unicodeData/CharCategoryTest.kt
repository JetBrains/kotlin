/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.unicodeData

import kotlin.text.unicode.*
import kotlin.test.*
import test.TestPlatform
import test.current

data class CharProperties(val code: Int, val categoryCode: String, val isStartOfARange: Boolean) {
    constructor(code: Int, categoryCode: String) : this(code, categoryCode, isStartOfARange = false)
}

@OptIn(ExperimentalUnicodeApi::class, ExperimentalKotlinTestApi::class)
abstract class CharCategoryTestBase(
    val isFull: Boolean,
    val charProperties: Map<Int, CharProperties>,
) {
    @Test
    fun category() {
        var properties: CharProperties? = null
        var testedCodePointCount = 0

        for (codePoint in CodePoint.MIN_VALUE..CodePoint.MAX_VALUE) {
            if (charProperties.containsKey(codePoint.code)) {
                properties = charProperties.getValue(codePoint.code)
            } else if (!isFull) {
                continue
            } else if (properties?.isStartOfARange != true) {
                properties = null
            }
            testedCodePointCount++

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
                    if (TestPlatform.current.isJvm() && !TestPlatform.current.isJvm(17)) {
                        // it's expected that different Unicode versions in other versions of JVM
                        // can have some characters unassigned or assigned
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
        println("Tested $testedCodePointCount code points")
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

    private fun isLowerCase(code: Int, categoryCode: String): Boolean {
        return categoryCode == CharCategory.LOWERCASE_LETTER.code || _otherLowerChars.contains(code)
    }

    private fun isUpperCase(code: Int, categoryCode: String): Boolean {
        return categoryCode == CharCategory.UPPERCASE_LETTER.code || _otherUpperChars.contains(code)
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
