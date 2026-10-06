/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text
import kotlin.text.unicode.*
import kotlin.test.*
import samples.*

@OptIn(ExperimentalUnicodeApi::class)
class CodePoints {

    @Sample
    fun basicProperties() {
        val basicCodePoint = 'K'.toCodePoint()
        assertPrints(basicCodePoint.toString(), "K")
        assertPrints(basicCodePoint.code.toHexString(), "0000004b")
        assertPrints(basicCodePoint.size, "1")
        assertTrue(basicCodePoint.isBasic)
        assertFalse(basicCodePoint.isSupplementary)

        val supplementaryCodePoint = CodePoint(0x1F60B) // 😋
        assertPrints(supplementaryCodePoint.toString(), "😋")
        assertPrints(supplementaryCodePoint.code.toHexString(), "0001f60b")
        assertPrints(supplementaryCodePoint.size, "2")
        assertFalse(supplementaryCodePoint.isBasic)
        assertTrue(supplementaryCodePoint.isSupplementary)
    }

    @Sample
    fun validatingConstructor() {
        val validCodePoint = CodePoint(0x1F60B) // 😋
        assertPrints(validCodePoint, "😋")

        assertFailsWith<IllegalArgumentException> { val invalidCodePoint = CodePoint(0x20FFFF) }
    }

    @Sample
    fun conversionFromInt() {
        val validCodePoint = 0x1F60B.toCodePoint() // 😋
        assertPrints(validCodePoint, "😋")

        val wrappedCodePoint = (-1).toCodePoint()
        assertPrints(wrappedCodePoint.code.toHexString(), "0010ffff")
    }

    @Sample
    fun conversionFromChar() {
        val char = 'Ü'
        val codePoint1 = char.toCodePoint()
        // or
        val codePoint2 = CodePoint.fromChar(char)

        assertPrints(codePoint1, "Ü")
        assertPrints(codePoint2, "Ü")
        // conversion from chars always result in basic code points
        assertPrints(codePoint1.size, "1")
        assertTrue(codePoint1.isBasic)
    }

    @Sample
    fun conversionFromSurrogatePair() {
        val high = '\uD83D'
        val low = '\uDE0B'
        val codePoint = CodePoint.fromSurrogatePair(high, low)

        assertPrints(codePoint, "😋")
        // conversion from surrogate pair always result in supplementary code points
        assertPrints(codePoint.size, "2")
        assertTrue(codePoint.isSupplementary)

        assertFailsWith<IllegalArgumentException> { CodePoint.fromSurrogatePair('a', low) }
    }

    @Sample
    fun conversionToSingleChar() {
        val basicCodePoint = 'Ü'.toCodePoint()
        val char = basicCodePoint.toSingleChar()

        assertPrints(char, "Ü")

        val supplementaryCodePoint = CodePoint(0x1F60B) // 😋
        assertFailsWith<IllegalArgumentException> { supplementaryCodePoint.toSingleChar() }
    }

    @Sample
    fun conversionToSurrogatePair() {
        val supplementaryCodePoint = CodePoint(0x1F60B) // 😋
        val [high, low] = supplementaryCodePoint.toSurrogatePair()

        // this code point is represented by surrogate pair \uD83D \uDE0B
        assertPrints(high.code.toHexString(), "0000d83d")
        assertPrints(low.code.toHexString(), "0000de0b")
        // or
        val string = supplementaryCodePoint.toSurrogatePair { h, l -> "$h$l" }
        assertPrints(string, "😋")

        val basicCodePoint = 'Ü'.toCodePoint()
        assertFailsWith<IllegalArgumentException> { basicCodePoint.toSurrogatePair() }
    }

    @Sample
    fun conversionToCharArray() {
        val basicCodePoint = 'K'.toCodePoint()
        assertPrints(basicCodePoint.toCharArray().joinToString { it.code.toHexString() }, "0000004b")

        val supplementaryCodePoint = CodePoint(0x1F60B) // 😋
        assertPrints(supplementaryCodePoint.toCharArray().joinToString { it.code.toHexString() }, "0000d83d, 0000de0b")
    }

    @Sample
    fun codePointRanges() {
        val rangeInclusive = 'A'.toCodePoint()..'Z'.toCodePoint()
        assertTrue('Z'.toCodePoint() in rangeInclusive)
        assertPrints(rangeInclusive.take(5).joinToString(), "A, B, C, D, E")

        val rangeExclusive = CodePoint(0x1F601)..<CodePoint(0x1F60B)
        assertPrints(rangeExclusive.joinToString(), "😁, 😂, 😃, 😄, 😅, 😆, 😇, 😈, 😉, 😊")
    }

    @Sample
    fun appendCodePoint() {
        val builder: Appendable = StringBuilder()
        builder.appendCodePoint('K'.toCodePoint()).appendCodePoint(CodePoint(0x1F60B))
        assertPrints(builder.toString(), "K😋")
    }

    @Sample
    fun insertCodePoint() {
        val builder = StringBuilder("😁😁 Kotlin")
        // suppose we want to insert another supplementary code point after the first two
        val position = builder.offsetByCodePoints(0, codePointOffset = 2)
        assertPrints(position, "4")
        builder.insert(position, CodePoint(0x1F60B))
        assertPrints(builder.toString(), "😁😁😋 Kotlin")
    }

    @Sample
    fun replaceCodePoint() {
        val builder = StringBuilder("😁😁 Kotlin")
        assertPrints(builder.length, "11") // 2 supplementary code points and 7 basic ones
        val position = builder.offsetByCodePoints(0, codePointOffset = 1)
        builder.setCodePointAt(position, '+'.toCodePoint())
        assertPrints(builder.toString(), "😁+ Kotlin")
        assertPrints(builder.length, "10") // 1 supplementary code points and 8 basic ones

        val position2 = builder.offsetByCodePoints(position, codePointOffset = 1)
        builder.setCodePointAt(position2, CodePoint(0x1F60B))
        assertPrints(builder.toString(), "😁+😋Kotlin")
        assertPrints(builder.length, "11") // again 2 supplementary code points and 7 basic ones
    }

    @Sample
    fun deleteCodePoint() {
        val builder = StringBuilder("😁😆 Kotlin")
        val position = builder.offsetByCodePoints(builder.indexOf(' '), codePointOffset = -1)
        builder.deleteCodePointAt(position)
        assertPrints(builder.toString(), "😁 Kotlin")
    }

    @Sample
    fun category() {
        val codePoint1 = CodePoint.fromChar('!')
        assertPrints(codePoint1.category, "OTHER_PUNCTUATION")
        assertTrue(codePoint1 in CharCategory.OTHER_PUNCTUATION)
        assertTrue(codePoint1 !in CharCategory.LOWERCASE_LETTER)

        val codePoint2 = CodePoint(0x1d7f9) // MATHEMATICAL MONOSPACE DIGIT THREE
        assertPrints(codePoint2.category, "DECIMAL_DIGIT_NUMBER")
    }

    @Sample
    fun isDigit() {
        assertTrue(CodePoint.fromChar('0').isDigit())
        assertTrue(CodePoint(0x1d7f9).isDigit()) // MATHEMATICAL MONOSPACE DIGIT THREE
        assertFalse(CodePoint.fromChar('Ü').isDigit())
    }

    @Sample
    fun isLetter() {
        assertTrue(CodePoint.fromChar('Ü').isLetter())
        assertFalse(CodePoint(0x1d7f9).isLetter()) // MATHEMATICAL MONOSPACE DIGIT THREE
        assertFalse(CodePoint.fromChar('!').isLetter())
    }

    @Sample
    fun isLetterOrDigit() {
        assertTrue(CodePoint.fromChar('0').isLetterOrDigit())
        assertTrue(CodePoint(0x1d7f9).isLetterOrDigit()) // MATHEMATICAL MONOSPACE DIGIT THREE
        assertTrue(CodePoint.fromChar('Ü').isLetterOrDigit())
        assertFalse(CodePoint.fromChar('!').isLetterOrDigit())
    }

    @Sample
    fun isISOControl() {
        val controlRange1 = '\u0000'.toCodePoint()..'\u001F'.toCodePoint()
        assertTrue(controlRange1.all { it.isISOControl() })

        val letterRange = 'a'.toCodePoint()..'z'.toCodePoint()
        assertTrue(letterRange.none { it.isISOControl() })
    }

    @Sample
    fun isLowerCase() {
        val codePoints = "1Aa+𐐀𐐨".asCodePointSequence()
        val [lowerCases, notLowerCases] = codePoints.partition { it.isLowerCase() }
        assertPrints(lowerCases, "[a, 𐐨]")
        assertPrints(notLowerCases, "[1, A, +, 𐐀]")
    }

    @Sample
    fun isUpperCase() {
        val codePoints = "1Aa+𐐀𐐨".asCodePointSequence()
        val [upperCases, notUpperCases] = codePoints.partition { it.isUpperCase() }
        assertPrints(upperCases, "[A, 𐐀]")
        assertPrints(notUpperCases, "[1, a, +, 𐐨]")
    }

    @Sample
    fun isTitleCase() {
        val codePoints = "ǅ_ǈ_ǋ_ǲ_1Aa+𐐀𐐨".asCodePointSequence()
        val [titleCases, notTitleCases] = codePoints.partition { it.isTitleCase() }
        assertPrints(titleCases, "[ǅ, ǈ, ǋ, ǲ]")
        assertPrints(notTitleCases, "[_, _, _, _, 1, A, a, +, 𐐀, 𐐨]")
    }

    @Sample
    fun isWhitespace() {
        val string = "\r\n😁\t\t\u00a0Kotlin"
        val withoutWhitespace = string.asCodePointSequence().filterNot { it.isWhitespace() }.joinToString("")
        assertPrints(withoutWhitespace, "😁Kotlin")
    }

    @Sample
    fun uppercase() {
        val codePoints = "aω1ŉA+ß𐐨".asCodePointSequence()
        val uppercaseCodePoints = codePoints.map { it.uppercaseCodePoint() }.toList()
        val uppercase = codePoints.map { it.uppercase() }.toList()
        assertPrints(uppercaseCodePoints, "[A, Ω, 1, ŉ, A, +, ß, 𐐀]")
        assertPrints(uppercase, "[A, Ω, 1, ʼN, A, +, SS, 𐐀]")
    }

    @Sample
    fun lowercase() {
        val codePoints = "AΩ1a+İ𐐀".asCodePointSequence()
        val lowercaseCodePoints = codePoints.map { it.lowercaseCodePoint() }.toList()
        val lowercase = codePoints.map { it.lowercase() }.toList()
        assertPrints(lowercaseCodePoints, "[a, ω, 1, a, +, i, 𐐨]")
        assertPrints(lowercase, "[a, ω, 1, a, +, \u0069\u0307, 𐐨]")
    }

    @Sample
    fun titlecase() {
        val codePoints = "a+ǅŉß𐐨".asCodePointSequence()
        val titlecaseCodePoints = codePoints.map { it.titlecaseCodePoint() }.toList()
        val titlecase = codePoints.map { it.titlecase() }.toList()
        assertPrints(titlecaseCodePoints, "[A, +, ǅ, ŉ, ß, 𐐀]")
        assertPrints(titlecase, "[A, +, ǅ, ʼN, Ss, 𐐀]")
    }
}
