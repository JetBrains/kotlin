/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.TestPlatform
import test.testExceptOn
import kotlin.text.unicode.*
import kotlin.test.*

@OptIn(ExperimentalUnicodeApi::class, ExperimentalKotlinTestApi::class)
class CodePointCasingTest {
    private val supplementaryCodePoints = CodePoint(Char.MAX_VALUE.code + 1)..CodePoint.MAX_VALUE
    private val uHexFormat = HexFormat {
        upperCase = true
        number {
            prefix = "U+"
            minLength = 4
            removeLeadingZeros = true
        }
    }
    private fun CodePoint.uString() = code.toHexString(uHexFormat)
    private fun String.uString() = asCodePointSequence().joinToString(" ") { it.uString() }

    private fun assertEquals(expected: CodePoint, actual: CodePoint, c: CodePoint) {
        assertEquals(expected, actual) { "c: ${c.uString()}, expected: ${expected.uString()}, actual: ${actual.uString()}" }
    }
    private fun assertEquals(expected: Char, actual: Char, c: CodePoint) {
        assertEquals(expected, actual) { "c: ${c.uString()}, expected: ${expected.toCodePoint().uString()}, actual: ${actual.toCodePoint().uString()}" }
    }
    private fun assertEquals(expected: String, actual: String, c: CodePoint) {
        assertEquals(expected, actual) { "c: ${c.uString()}, expected: ${expected.uString()}, actual: ${actual.uString()}" }
    }

    private fun testRange(test: (CodePoint, CodePoint) -> Unit, range: Iterable<CodePoint>, delta: Int) {
        for (c in range) {
            test(c, c + delta)
            test(c + delta, c + delta)
        }
    }

    private fun testLowercase(c: Char, expectedResult: Char, expectedStringResult: String = expectedResult.toString()) {
        testLowercase(c.toCodePoint(), expectedResult.toCodePoint(), expectedStringResult)
    }

    private fun testLowercase(c: CodePoint, expectedResult: CodePoint, expectedStringResult: String = expectedResult.toString()) {
        assertEquals(expectedResult, c.lowercaseCodePoint(), c)
        assertEquals(expectedStringResult, c.lowercase(), c)
        assertEquals(expectedStringResult, c.toString().lowercase(), c)

        if (c.isBasic) {
            assertEquals(expectedResult.toSingleChar(), c.toSingleChar().lowercaseChar(), c)
            assertEquals(expectedStringResult, c.toSingleChar().lowercase(), c)
        }
    }

    @Test
    fun lowercase() {
        testLowercase('\u0000', '\u0000')

        // ASCII
        testLowercase('\u0040', '\u0040')
        testRange(::testLowercase, 'A'.toCodePoint()..'Z'.toCodePoint(), 0x20)
        testLowercase('\u005B', '\u005B')

        // <Lu, Ll>
        testRange(::testLowercase, CodePoint(0x0100)..CodePoint(0x012E) step 2, 1)

        // LATIN CAPITAL LETTER I WITH DOT ABOVE
        testLowercase('\u0130', '\u0069', "\u0069\u0307")

        // <Lu, Lt, Ll>
        testRange(::testLowercase, CodePoint(0x01C4)..CodePoint(0x01CA) step 3, 2)
        testRange(::testLowercase, CodePoint(0x01C5)..CodePoint(0x01CB) step 3, 1)

        // large mapping
        testLowercase('\u0239', '\u0239')
        testLowercase('\u023A', '\u2C65')
        testLowercase('\u023B', '\u023C')

        // large negative mapping
        testLowercase('\u2C7D', '\u2C7D')
        testLowercase('\u2C7E', '\u023F')
        testLowercase('\u2C7F', '\u0240')

        // <Lu, Ll>
        testLowercase('\u2C80', '\u2C81')
        testLowercase('\u2C81', '\u2C81')
        testLowercase('\u2C82', '\u2C83')

        // Lo, Myanmar script
        testRange(::testLowercase, CodePoint(0x1000)..CodePoint(0x102A), 0)

        // last mappings
        testLowercase('\uFF20', '\uFF20')
        testLowercase('\uFF21', '\uFF41')
        testLowercase('\uFF3A', '\uFF5A')
        testLowercase('\uFF3B', '\uFF3B')

        testLowercase('\uFFFF', '\uFFFF')

        testRange(::testLowercase, (CodePoint(0x10A0)..CodePoint(0x10C5)) + CodePoint(0x10C7) + CodePoint(0x10CD), 0x1C60)

        // Deseret Script
        testRange(::testLowercase, CodePoint(0x10400)..CodePoint(0x10427), 0x28)


        // mappings not available in JDK 8' version of Unicode
        testExceptOn(TestPlatform.Jvm) {
            testRange(::testLowercase, ((0x1C90..0x1CBF) - 0x1CBB - 0x1CBC).map(::CodePoint), -0xBC0)

            testRange(::testLowercase, CodePoint(0x10C80)..CodePoint(0x10CB2), 0x40)
            testRange(::testLowercase, CodePoint(0x1E900)..CodePoint(0x1E921), 0x22)
        }

        for (c in supplementaryCodePoints) {
            assertEquals(c.lowercaseCodePoint().toString(), c.lowercase(), c)
            assertTrue(c.lowercaseCodePoint().isSupplementary) { "c: ${c.uString()}, actual: ${c.lowercase().uString()}" }
        }
    }

    private fun testUppercase(c: Char, expectedResult: Char, expectedStringResult: String = expectedResult.toString()) {
        testUppercase(c.toCodePoint(), expectedResult.toCodePoint(), expectedStringResult)
    }

    private fun testUppercase(c: CodePoint, expectedResult: CodePoint, expectedStringResult: String = expectedResult.toString()) {
        assertEquals(expectedResult, c.uppercaseCodePoint(), c)
        assertEquals(expectedStringResult, c.uppercase(), c)
        assertEquals(expectedStringResult, c.toString().uppercase(), c)

        if (c.isBasic) {
            assertEquals(expectedResult.toSingleChar(), c.toSingleChar().uppercaseChar(), c)
            assertEquals(expectedStringResult, c.toSingleChar().uppercase(), c)
        }
    }

    @Test
    fun uppercase() {
        testUppercase('\u0000', '\u0000')

        // ASCII
        testUppercase('\u0060', '\u0060')
        testRange(::testUppercase, 'a'.toCodePoint()..'z'.toCodePoint(), -0x20)

        testUppercase('\u007B', '\u007B')

        // LATIN SMALL LETTER SHARP S (ß -> SS)
        testUppercase('\u00DF', '\u00DF', "SS")

        // <Lu, Ll>
        testRange(::testUppercase, CodePoint(0x101)..CodePoint(0x12F) step 2, -1)

        // LATIN CAPITAL LETTER I WITH DOT ABOVE
        testUppercase('\u0130', '\u0130')
        // LATIN SMALL LETTER N PRECEDED BY APOSTROPHE (ŉ -> ʼN)
        testUppercase('\u0149', '\u0149', "\u02BC\u004E")

        // <Ll, x, Ll, ...>
        testUppercase('\u1F50', '\u1F50', "\u03A5\u0313")
        testUppercase('\u1F51', '\u1F59')
        testUppercase('\u1F52', '\u1F52', "\u03A5\u0313\u0300")
        testUppercase('\u1F57', '\u1F5F')
        testUppercase('\u1F58', '\u1F58')

        // <Lu, Lt, Ll>
        testRange(::testUppercase, CodePoint(0x01C5)..CodePoint(0x01CB) step 3, -1)
        testRange(::testUppercase, CodePoint(0x01C6)..CodePoint(0x01CC) step 3, -2)

        // Lo, Myanmar script
        testRange(::testUppercase, CodePoint(0x1000)..CodePoint(0x102A), 0)

        // ligatures
        testUppercase('\uFB03', '\uFB03', "FFI")
        testUppercase('\uFB04', '\uFB04', "FFL")
        testUppercase('\u0587', '\u0587', "\u0535\u0552")


        // last BMP mappings
        testUppercase('\uFF40', '\uFF40')
        testUppercase('\uFF41', '\uFF21')
        testUppercase('\uFF5A', '\uFF3A')
        testUppercase('\uFF5B', '\uFF5B')

        testUppercase('\uFFFF', '\uFFFF')

        // Deseret Script
        testRange(::testUppercase, CodePoint(0x10428)..CodePoint(0x1044F), -0x28)

        // mappings not available in JDK 8' version of Unicode
        testExceptOn(TestPlatform.Jvm) {
            // large mapping
            testUppercase('\u029C', '\u029C')
            testUppercase('\u029D', '\uA7B2')
            testUppercase('\u029E', '\uA7B0')
            testUppercase('\u029F', '\u029F')

            testRange(::testUppercase, ((0x10D0..0x10FF) - (0x10FB..0x10FC)).map(::CodePoint), 0xBC0)
            testRange(::testUppercase, ((0x2D00..0x2D25) + 0x2D27 + 0x2D2D).map(::CodePoint), -0x1C60)

            // large negative mapping
            testUppercase('\uAB6F', '\uAB6F')
            testRange(::testUppercase, CodePoint(0xAB70)..CodePoint(0xABBF), -0x97D0)
            testUppercase('\uABC0', '\uABC0')

            testRange(::testUppercase, CodePoint(0x10CC0)..CodePoint(0x10CF2), -0x40)
            testRange(::testUppercase, CodePoint(0x1E922)..CodePoint(0x1E943), -0x22)
        }

        for (c in supplementaryCodePoints) {
            assertEquals(c.uppercaseCodePoint().toString(), c.uppercase(), c)
            assertTrue(c.uppercaseCodePoint().isSupplementary) { "c: ${c.uString()}, actual: ${c.uppercase().uString()}" }
        }
    }

    private fun testTitlecaseEquivalentToUppercase(range: Iterable<CodePoint>) {
        for (c in range) {
            if (c.isBasic) {
                testTitlecase(c.toSingleChar(), c.toSingleChar().uppercaseChar(), c.toSingleChar().uppercase())
            } else {
                testTitlecase(c, c.uppercaseCodePoint())
            }
        }
    }

    private fun testTitlecase(c: Char, expectedResult: Char, expectedStringResult: String = expectedResult.toString()) {
        testTitlecase(c.toCodePoint(), expectedResult.toCodePoint(), expectedStringResult)
    }

    private fun testTitlecase(c: CodePoint, expectedResult: CodePoint, expectedStringResult: String = expectedResult.toString()) {
        assertEquals(expectedResult, c.titlecaseCodePoint(), c)
        assertEquals(expectedStringResult, c.titlecase(), c)

        if (c.isBasic) {
            assertEquals(expectedResult.toSingleChar(), c.toSingleChar().titlecaseChar(), c)
            assertEquals(expectedStringResult, c.toSingleChar().titlecase(), c)
        }
    }

    @Test
    fun titlecase() {
        // ASCII
        testTitlecaseEquivalentToUppercase(CodePoint(0x0000)..CodePoint(0x007F))

        // LATIN SMALL LETTER SHARP S (ß -> Ss)
        testTitlecase('\u00DF', '\u00DF', "Ss")
        // LATIN SMALL LETTER N PRECEDED BY APOSTROPHE (ŉ -> ʼN)
        testTitlecase('\u0149', '\u0149', "\u02BC\u004E")

        // <Lu, Lt, Ll>
        testRange(::testTitlecase, CodePoint(0x01C4)..CodePoint(0x01CA) step 3, 1)
        testRange(::testTitlecase, CodePoint(0x01C6)..CodePoint(0x01CC) step 3, -1)

        // Lu, Lt, Ll
        testTitlecase('\u01F0', '\u01F0', "\u004A\u030C")
        testTitlecase('\u01F1', '\u01F2')
        testTitlecase('\u01F2', '\u01F2')
        testTitlecase('\u01F3', '\u01F2')
        testTitlecase('\u01F4', '\u01F4')

        testTitlecaseEquivalentToUppercase(CodePoint(0x0560)..CodePoint(0x0586))
        testTitlecase('\u0587', '\u0587', "\u0535\u0582")

        // titlecase != uppercase
        testRange(::testTitlecase, CodePoint(0x10D0)..CodePoint(0x10FF), 0)

        // ligatures
        testTitlecase('\uFB03', '\uFB03', "Ffi")
        testTitlecase('\uFB04', '\uFB04', "Ffl")

        // titlecaseChar == uppercaseChar
        testTitlecaseEquivalentToUppercase(CodePoint(0xA640)..CodePoint(0xA69B))

        testTitlecase('\uFFFF', '\uFFFF')

        // all title case mappings are same as upper case in supplementary planes
        testTitlecaseEquivalentToUppercase(supplementaryCodePoints)
    }
}
