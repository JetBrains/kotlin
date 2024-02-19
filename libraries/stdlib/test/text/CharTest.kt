/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class CharTest {

    companion object {
        val equalIgnoreCaseGroups = listOf(
            "Aa", "Zz", "üÜ", "öÖ", "äÄ",
            "KkK", "Ssſ", "µΜμ", "ÅåÅ",
            "Ǆǅǆ", "Ǉǈǉ", "Ǌǋǌ", "Ǳǲǳ", "ͅΙιι", "Ββϐ", "Εεϵ",
            "Κκϰ", "Ππϖ", "Ρρϱ", "Σςσ", "Φφϕ", "ΩωΩ", "Ṡṡẛ",
            "Θθϑϴ", "Iiİı",
        )

       val allCharsByCategory = (Char.MIN_VALUE..Char.MAX_VALUE).groupBy { it.category }
    }

    @Test
    fun charFromIntCode() {
        val codes = listOf(48, 65, 122, 946, '+'.code, 'Ö'.code, 0xFFFC)
        assertEquals("0Azβ+Ö\uFFFC", codes.map { Char(it) }.joinToString(separator = ""))

        assertFails { Char(-1) }
        assertFails { Char(0x1_0000) }
        assertFails { Char(1_000_000) }
    }

    @Test
    fun charFromUShortCode() {
        val codes = listOf(48, 65, 122, 946, '+'.code, 'Ö'.code, 0xFFFC)
        assertEquals("0Azβ+Ö\uFFFC", codes.map { Char(it.toUShort()) }.joinToString(separator = ""))

        assertEquals('\u0000', Char(UShort.MIN_VALUE))
        assertEquals('\uFFFF', Char(UShort.MAX_VALUE))
        assertEquals('\uFFFF', Char((-1).toUShort()))
        assertEquals('\u0000', Char(0x1_0000.toUShort()))
    }

    @Test
    fun code() {
        val codes = listOf(48, 65, 122, 946, '+'.code, 'Ö'.code, 0xFFFC)
        val chars = "0Azβ+Ö\uFFFC"
        assertEquals(codes, chars.map { it.code })
        assertEquals(0, Char.MIN_VALUE.code)
        assertEquals(0xFFFF, Char.MAX_VALUE.code)
    }

    @Test
    fun digitToInt() {
        fun testEquals(expected: Int, receiver: Char, radix: Int) {
            if (radix == 10) {
                assertEquals(expected, receiver.digitToInt())
                assertEquals(expected, receiver.digitToIntOrNull())
            }
            assertEquals(expected, receiver.digitToInt(radix))
            assertEquals(expected, receiver.digitToIntOrNull(radix))
        }

        fun testFails(receiver: Char, radix: Int) {
            if (radix == 10) {
                assertFails { receiver.digitToInt() }
                assertEquals(null, receiver.digitToIntOrNull())
            }
            assertFails { receiver.digitToInt(radix) }
            assertEquals(null, receiver.digitToIntOrNull(radix))
        }

        for (char in '0'..'9') {
            val digit = char - '0'

            for (radix in (digit + 1).coerceAtLeast(2)..36) {
                testEquals(digit, char, radix)
            }
            for (radix in 2..digit) {
                testFails(char, radix)
            }
        }

        val letterRanges = listOf('A'..'Z', '\uFF21'..'\uFF3A')

        for (range in letterRanges) {
            for (char in range) {
                val digit = 10 + (char - range.first)
                val lower = char.lowercaseChar()

                for (radix in digit + 1..36) {
                    testEquals(digit, char, radix)
                    testEquals(digit, lower, radix)
                }
                for (radix in 2..digit) {
                    testFails(char, radix)
                    testFails(lower, radix)
                }
            }
        }

        assertFails { '0'.digitToInt(radix = 37) }
        assertFails { '0'.digitToIntOrNull(radix = 37) }
        assertFails { '0'.digitToInt(radix = 1) }
        assertFails { '0'.digitToIntOrNull(radix = 1) }

        testFails('0' - 1, radix = 10)
        testFails('9' + 1, radix = 10)
        testFails('β', radix = 36)
        testFails('+', radix = 36)
    }

    @Test
    fun digitToChar() {
        fun testEquals(expected: Char, receiver: Int, radix: Int) {
            if (radix == 10) {
                assertEquals(expected, receiver.digitToChar())
            }
            assertEquals(expected, receiver.digitToChar(radix))
        }

        fun testFails(receiver: Int, radix: Int) {
            if (radix == 10) {
                assertFails { receiver.digitToChar() }
            }
            assertFails { receiver.digitToChar(radix) }
        }

        for (int in 0..9) {
            val digit = '0' + int

            for (radix in (int + 1).coerceAtLeast(2)..36) {
                testEquals(digit, int, radix)
            }
            for (radix in 2..int) {
                testFails(int, radix)
            }

            testFails(int, radix = 37)
        }

        for (int in 10..35) {
            val digit = 'A' + (int - 10)

            for (radix in int + 1..36) {
                testEquals(digit, int, radix)
            }
            for (radix in 2..int) {
                testFails(int, radix)
            }

            testFails(int, radix = 37)
        }

        assertFails { 0.digitToChar(radix = 37) }
        assertFails { 0.digitToChar(radix = 1) }

        testFails(-1, radix = 10)
        testFails(100, radix = 36)
        testFails(100, radix = 110)
    }

    @Test
    fun equalsIgnoreCase() {
        val nonEqual = equalIgnoreCaseGroups.flatMap { allEqualChars ->
            allEqualChars.flatMap { c1 -> allEqualChars.mapNotNull { c2 ->
                    if (!c1.equals(c2, ignoreCase = true)) "$c1 != $c2" else null
                }
            }
        }
        if (nonEqual.isNotEmpty()) {
            fail("Expected chars to be equal ignoring case:\n${nonEqual.joinToString("\n")}")
        }
    }


    private fun charToCategory() = mapOf(
        '\u0378' to "Cn",
        'A' to "Lu",    // \u0041
        'a' to "Ll",    // \u0061
        'ǅ' to "Lt",    // \u01C5
        'ʰ' to "Lm",    // \u02B0
        'ƻ' to "Lo",    // \u01BB
        '\u0300' to "Mn",
        '\u0489' to "Me",
        '\u0903' to "Mc",
        '0' to "Nd",    // \u0030
        'Ⅰ' to "Nl",    // \u2160
        '²' to "No",    // \u00B2
        ' ' to "Zs",    // \u0020
        '\u2028' to "Zl",
        '\u2029' to "Zp",
        '\u0018' to "Cc",
        '\u00AD' to "Cf",
        '\uE000' to "Co",
        '\uD800' to "Cs",
        '\u002D' to "Pd",
        '(' to "Ps",    // \u0028
        ')' to "Pe",    // \u0029
        '_' to "Pc",    // \u005F
        '!' to "Po",    // \u0021
        '+' to "Sm",    // \u002B
        '$' to "Sc",    // \u0024
        '^' to "Sk",    // \u005E
        '©' to "So",    // \u00A9
        '«' to "Pi",    // \u00AB
        '»' to "Pf"     // \u00BB
    )

    @Test
    fun charCategory() {
        for ((char, categoryCode) in charToCategory()) {
            assertEquals(categoryCode, char.category.code, "char code: ${char.code.toString(radix = 16)}")
        }
    }

    @Test
    fun charCategoryUnassigned() {
        val unassignedChar = '\u0378'
        assertFalse(unassignedChar.isDefined())
        assertEquals(CharCategory.UNASSIGNED, unassignedChar.category)
        assertEquals("Cn", CharCategory.UNASSIGNED.code)
    }

    @Test
    fun charCategoryUppercaseLetter() {
        val latinCapitalLetterA = 'A' // \u0041
        assertTrue(latinCapitalLetterA.isLetterOrDigit())
        assertTrue(latinCapitalLetterA.isLetter())
        assertTrue(latinCapitalLetterA.isUpperCase())
        assertEquals(CharCategory.UPPERCASE_LETTER, latinCapitalLetterA.category)
        assertEquals("Lu", CharCategory.UPPERCASE_LETTER.code)
    }

    @Test
    fun charCategoryLowercaseLetter() {
        val latinSmallLetterA = 'a' // \u0061
        assertTrue(latinSmallLetterA.isLetterOrDigit())
        assertTrue(latinSmallLetterA.isLetter())
        assertTrue(latinSmallLetterA.isLowerCase())
        assertEquals(CharCategory.LOWERCASE_LETTER, latinSmallLetterA.category)
        assertEquals("Ll", CharCategory.LOWERCASE_LETTER.code)
    }

    @Test
    fun charCategoryTitlecaseLetter() {
        val latinCapitalLetterDz = 'ǅ' // \u01C5
        assertTrue(latinCapitalLetterDz.isLetterOrDigit())
        assertTrue(latinCapitalLetterDz.isLetter())
        assertTrue(latinCapitalLetterDz.isTitleCase())
        assertEquals(CharCategory.TITLECASE_LETTER, latinCapitalLetterDz.category)
        assertEquals("Lt", CharCategory.TITLECASE_LETTER.code)
    }

    @Test
    fun charCategoryModifierLetter() {
        val modifierLetterSmallH = 'ʰ' // \u02B0
        assertTrue(modifierLetterSmallH.isLetterOrDigit())
        assertTrue(modifierLetterSmallH.isLetter())
        assertEquals(CharCategory.MODIFIER_LETTER, modifierLetterSmallH.category)
        assertEquals("Lm", CharCategory.MODIFIER_LETTER.code)
    }

    @Test
    fun charCategoryOtherLetter() {
        val twoWithStroke = 'ƻ' // \u01BB
        assertTrue(twoWithStroke.isLetterOrDigit())
        assertTrue(twoWithStroke.isLetter())
        assertEquals(CharCategory.OTHER_LETTER, twoWithStroke.category)
        assertEquals("Lo", CharCategory.OTHER_LETTER.code)
    }

    @Test
    fun charCategoryDecimalDigitNumber() {
        val digitZero = '0' // \u0030
        assertTrue(digitZero.isLetterOrDigit())
        assertTrue(digitZero.isDigit())
        assertEquals(CharCategory.DECIMAL_DIGIT_NUMBER, digitZero.category)
        assertEquals("Nd", CharCategory.DECIMAL_DIGIT_NUMBER.code)
    }

    @Test
    fun charCategoryLetterNumber() {
        val romanNumberOne = 'Ⅰ' // \u2160
        assertFalse(romanNumberOne.isDigit())
        assertEquals(CharCategory.LETTER_NUMBER, romanNumberOne.category)
        assertEquals("Nl", CharCategory.LETTER_NUMBER.code)
    }

    @Test
    fun charCategoryOtherNumber() {
        val superscriptTwo = '²' // \u00B2
        assertFalse(superscriptTwo.isDigit())
        assertEquals(CharCategory.OTHER_NUMBER, superscriptTwo.category)
        assertEquals("No", CharCategory.OTHER_NUMBER.code)
    }

    @Test
    fun charCategorySpaceSeparator() {
        val superscriptTwo = ' ' // \u0020
        assertTrue(superscriptTwo.isWhitespace())
        assertEquals(CharCategory.SPACE_SEPARATOR, superscriptTwo.category)
        assertEquals("Zs", CharCategory.SPACE_SEPARATOR.code)
    }

    @Test
    fun charCategoryLineSeparator() {
        val lineSeparator = '\u2028'
        assertTrue(lineSeparator.isWhitespace())
        assertEquals(CharCategory.LINE_SEPARATOR, lineSeparator.category)
        assertEquals("Zl", CharCategory.LINE_SEPARATOR.code)
    }

    @Test
    fun charCategoryParagraphSeparator() {
        val paragraphSeparator = '\u2029'
        assertTrue(paragraphSeparator.isWhitespace())
        assertEquals(CharCategory.PARAGRAPH_SEPARATOR, paragraphSeparator.category)
        assertEquals("Zp", CharCategory.PARAGRAPH_SEPARATOR.code)
    }

    @Test
    fun charCategoryControl() {
        val controlCancel = '\u0018'
        assertTrue(controlCancel.isISOControl())
        assertEquals(CharCategory.CONTROL, controlCancel.category)
        assertEquals("Cc", CharCategory.CONTROL.code)
    }

    @Test
    fun whitespace() {
        val allWhitespace = (Char.MIN_VALUE..Char.MAX_VALUE).filter { it.isWhitespace() }
        val expected =
            listOf(CharCategory.SPACE_SEPARATOR, CharCategory.LINE_SEPARATOR, CharCategory.PARAGRAPH_SEPARATOR)
                .flatMap { allCharsByCategory[it]!! } +
                    ('\u0009'..'\u000D') +
                    ('\u001C'..'\u001F')
        assertEquals(expected.sorted(), allWhitespace)
    }

    @Test
    fun lowercaseChar() {
        assertEquals('\u0000', '\u0000'.lowercaseChar())

        // ASCII
        assertEquals('\u0040', '\u0040'.lowercaseChar())
        for (index in 0..25) { // '\u0041'..'\u005A' -> '\u0061'..'\u007A'
            assertEquals('a' + index, ('A' + index).lowercaseChar())
            assertEquals('a' + index, ('a' + index).lowercaseChar())
        }
        assertEquals('\u005B', '\u005B'.lowercaseChar())

        // <Lu, Ll>
        assertEquals('\u0101', '\u0100'.lowercaseChar())
        assertEquals('\u0101', '\u0101'.lowercaseChar())
        assertEquals('\u0103', '\u0102'.lowercaseChar())

        // LATIN CAPITAL LETTER I WITH DOT ABOVE
        assertEquals('\u0069', '\u0130'.lowercaseChar())

        // last mappings
        assertEquals('\uFF20', '\uFF20'.lowercaseChar())
        assertEquals('\uFF41', '\uFF21'.lowercaseChar())
        assertEquals('\uFF5A', '\uFF3A'.lowercaseChar())
        assertEquals('\uFF3B', '\uFF3B'.lowercaseChar())

        assertEquals('\uFFFF', '\uFFFF'.lowercaseChar())
    }

    @Test
    fun uppercaseChar() {
        assertEquals('\u0000', '\u0000'.uppercaseChar())

        // ASCII
        assertEquals('\u0060', '\u0060'.uppercaseChar())
        for (index in 0..25) { // '\u0061'..'\u007A' -> '\u0041'..'\u005A'
            assertEquals('A' + index, ('a' + index).uppercaseChar())
            assertEquals('A' + index, ('A' + index).uppercaseChar())
        }
        assertEquals('\u007B', '\u007B'.uppercaseChar())

        // <Lu, Ll>
        assertEquals('\u012C', '\u012D'.uppercaseChar())
        assertEquals('\u012E', '\u012E'.uppercaseChar())
        assertEquals('\u012E', '\u012F'.uppercaseChar())

        // LATIN CAPITAL LETTER I WITH DOT ABOVE
        assertEquals('\u0130', '\u0130'.uppercaseChar())

        // <Ll, x, Ll, ...>
        assertEquals('\u1F50', '\u1F50'.uppercaseChar())
        assertEquals('\u1F59', '\u1F51'.uppercaseChar())
        assertEquals('\u1F5F', '\u1F57'.uppercaseChar())
        assertEquals('\u1F58', '\u1F58'.uppercaseChar())

        // last mappings
        assertEquals('\uFF40', '\uFF40'.uppercaseChar())
        assertEquals('\uFF21', '\uFF41'.uppercaseChar())
        assertEquals('\uFF3A', '\uFF5A'.uppercaseChar())
        assertEquals('\uFF5B', '\uFF5B'.uppercaseChar())

        assertEquals('\uFFFF', '\uFFFF'.uppercaseChar())
    }

    @Test
    fun titlecaseChar() {
        assertEquals('\u0000', '\u0000'.titlecaseChar())

        // ASCII
        assertEquals('\u0060', '\u0060'.titlecaseChar())
        for (index in 0..25) { // '\u0061'..'\u007A' -> '\u0041'..'\u005A'
            assertEquals('A' + index, ('a' + index).titlecaseChar())
            assertEquals('A' + index, ('A' + index).titlecaseChar())
        }
        assertEquals('\u007B', '\u007B'.titlecaseChar())

        // <Lu, Lt, Ll>
        assertEquals('\u01C5', '\u01C4'.titlecaseChar())
        assertEquals('\u01C5', '\u01C5'.titlecaseChar())
        assertEquals('\u01C5', '\u01C6'.titlecaseChar())
        assertEquals('\u01C8', '\u01C7'.titlecaseChar())
        assertEquals('\u01C8', '\u01C8'.titlecaseChar())
        assertEquals('\u01C8', '\u01C9'.titlecaseChar())
        assertEquals('\u01CB', '\u01CA'.titlecaseChar())
        assertEquals('\u01CB', '\u01CB'.titlecaseChar())
        assertEquals('\u01CB', '\u01CC'.titlecaseChar())

        // Lu, Lt, Ll
        assertEquals('\u01F0', '\u01F0'.titlecaseChar())
        assertEquals('\u01F2', '\u01F1'.titlecaseChar())
        assertEquals('\u01F2', '\u01F2'.titlecaseChar())
        assertEquals('\u01F2', '\u01F3'.titlecaseChar())
        assertEquals('\u01F4', '\u01F4'.titlecaseChar())

        // titlecaseChar == uppercaseChar
        assertEquals('\uA68D'.uppercaseChar(), '\uA68D'.titlecaseChar())
        assertEquals('\uA7C3'.uppercaseChar(), '\uA7C3'.titlecaseChar())

        assertEquals('\uFFFF', '\uFFFF'.titlecaseChar())
    }

    @Test
    fun lowercase() {
        assertEquals("\u0000", '\u0000'.lowercase())

        // ASCII
        assertEquals("\u0040", "\u0040".lowercase())
        for (index in 0..25) { // '\u0041'..'\u005A' -> '\u0061'..'\u007A'
            assertEquals(('a' + index).toString(), ('A' + index).lowercase())
            assertEquals(('a' + index).toString(), ('a' + index).lowercase())
        }
        assertEquals("\u005B", '\u005B'.lowercase())

        // lowercase = lowercaseChar != char
        assertEquals('\u04A6'.lowercaseChar().toString(), '\u04A6'.lowercase())
        assertNotEquals("\u04A6", '\u04A6'.lowercase())

        // lowercase = lowercaseChar = char
        assertEquals('\u2CE8'.lowercaseChar().toString(), '\u2CE8'.lowercase())
        assertEquals("\u2CE8", '\u2CE8'.lowercase())

        assertEquals("\uFFFF", '\uFFFF'.lowercase())
    }

    @Test
    fun uppercase() {
        assertEquals("\u0000", '\u0000'.uppercase())

        // ASCII
        assertEquals("\u0060", '\u0060'.uppercase())
        for (index in 0..25) { // '\u0061'..'\u007A' -> '\u0041'..'\u005A'
            assertEquals(('A' + index).toString(), ('a' + index).uppercase())
            assertEquals(('A' + index).toString(), ('A' + index).uppercase())
        }
        assertEquals("\u007B", '\u007B'.uppercase())

        // LATIN SMALL LETTER SHARP S (ß -> SS)
        assertEquals("\u0053\u0053", '\u00df'.uppercase())

        // LATIN SMALL LETTER N PRECEDED BY APOSTROPHE (ŉ -> ʼN)
        assertEquals("\u02BC\u004E", '\u0149'.uppercase())

        // LATIN SMALL LIGATURE FFI (ﬃ -> FFI)
        assertEquals("\u0046\u0046\u0049", '\uFB03'.uppercase())

        // uppercase = uppercaseChar != char
        assertEquals('\u056C'.uppercaseChar().toString(), '\u056C'.uppercase())
        assertNotEquals("\u056C", '\u056C'.uppercase())

        // uppercase = uppercaseChar == char
        assertEquals('\u1000'.uppercaseChar().toString(), '\u1000'.uppercase())
        assertEquals("\u1000", '\u1000'.uppercase())

        assertEquals("\uFFFF", '\uFFFF'.uppercase())
    }

    @Test
    fun titlecase() {
        assertEquals("\u0000", '\u0000'.titlecase())

        // ASCII
        assertEquals("\u0060", '\u0060'.titlecase())
        for (index in 0..25) { // '\u0061'..'\u007A' -> '\u0041'..'\u005A'
            assertEquals(('A' + index).toString(), ('a' + index).titlecase())
            assertEquals(('A' + index).toString(), ('A' + index).titlecase())
        }
        assertEquals("\u007B", '\u007B'.titlecase())

        // LATIN SMALL LETTER SHARP S (ß -> Ss)
        assertEquals("\u0053\u0073", '\u00df'.titlecase())

        // LATIN SMALL LETTER N PRECEDED BY APOSTROPHE (ŉ -> ʼN)
        assertEquals("\u02BC\u004E", '\u0149'.titlecase())

        // LATIN SMALL LIGATURE FFI (ﬃ -> Ffi)
        assertEquals("\u0046\u0066\u0069", '\uFB03'.titlecase())

        // titlecase = titlecaseChar = uppercaseChar != char
        assertEquals('\u056C'.titlecaseChar().toString(), '\u056C'.titlecase())
        assertEquals('\u056C'.uppercaseChar(), '\u056C'.titlecaseChar())
        assertNotEquals('\u056C', '\u056C'.uppercaseChar())

        // titlecase = titlecaseChar != uppercaseChar != char
        assertEquals('\u01C6'.titlecaseChar().toString(), '\u01C6'.titlecase())
        assertNotEquals('\u01C6'.uppercaseChar(), '\u01C6'.titlecaseChar())
        assertNotEquals('\u01C6', '\u01C6'.titlecaseChar())
        assertNotEquals('\u01C6', '\u01C6'.uppercaseChar())

        // titlecase = titlecaseChar = uppercaseChar = char
        assertEquals('\u1000'.titlecaseChar().toString(), '\u1000'.titlecase())
        assertEquals('\u1000'.uppercaseChar(), '\u1000'.titlecaseChar())
        assertEquals('\u1000', '\u1000'.uppercaseChar())

        assertEquals("\uFFFF", '\uFFFF'.titlecase())
    }

    @Test
    fun otherLowercaseProperty() {
        val feminineOrdinalIndicator = '\u00AA'
        assertTrue(feminineOrdinalIndicator.isLowerCase())
        assertTrue(feminineOrdinalIndicator.isLetter())
        assertFalse(feminineOrdinalIndicator.isUpperCase())

        val circledLatinSmallLetterA = '\u24D0'
        assertTrue(circledLatinSmallLetterA.isLowerCase())
        assertFalse(circledLatinSmallLetterA.isLetter())
        assertFalse(circledLatinSmallLetterA.isUpperCase())
    }

    @Test
    fun otherUppercaseProperty() {
        val romanNumberOne = '\u2160'
        assertTrue(romanNumberOne.isUpperCase())
        assertFalse(romanNumberOne.isLetter())
        assertFalse(romanNumberOne.isLowerCase())

        val circledLatinCapitalLetterZ = '\u24CF'
        assertTrue(circledLatinCapitalLetterZ.isUpperCase())
        assertFalse(circledLatinCapitalLetterZ.isLetter())
        assertFalse(circledLatinCapitalLetterZ.isLowerCase())
    }

    @Test
    fun isHighSurrogate() {
        assertTrue('\uD800'.isHighSurrogate())
        assertTrue('\uDBFF'.isHighSurrogate())
        assertFalse('\uDC00'.isHighSurrogate())
        assertFalse('\uDFFF'.isHighSurrogate())
    }

    @Test
    fun isLowSurrogate() {
        assertFalse('\uD800'.isLowSurrogate())
        assertFalse('\uDBFF'.isLowSurrogate())
        assertTrue('\uDC00'.isLowSurrogate())
        assertTrue('\uDFFF'.isLowSurrogate())
    }
}
