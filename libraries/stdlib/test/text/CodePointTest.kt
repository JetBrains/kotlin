/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.codepoints

import kotlin.text.unicode.*
import kotlin.math.sign
import kotlin.test.*

@OptIn(ExperimentalUnicodeApi::class)
class CodePointTest {

    companion object {
        val bmpCodePointCodes = listOf(0, 1, '0'.code, 'a'.code, 0xFF, Char.MIN_SURROGATE.code, Char.MAX_VALUE.code)
        val supplementaryCodePointCodes = listOf(Char.MAX_VALUE.code + 1, 0x1F600, CodePoint.MAX_VALUE.code)

        val bmpCodePoints by lazy { bmpCodePointCodes.map(::CodePoint) }
        val supplementaryCodePoints by lazy { supplementaryCodePointCodes.map(::CodePoint) }
    }

    @Test
    fun codePointConstruction() {
        val validCodePoints = bmpCodePointCodes + supplementaryCodePointCodes
        val invalidCodePoints =
            validCodePoints.map { it - (CodePoint.MAX_VALUE.code + 1) } +
            validCodePoints.map { it + (CodePoint.MAX_VALUE.code + 1) } +
            Int.MAX_VALUE

        for (code in validCodePoints) {
            val c1 = CodePoint(code)
            val c2 = code.toCodePoint()

            assertEquals(c1, c2)
            assertEquals(code, c1.code)
        }

        for (code in invalidCodePoints) {
            assertFailsWith<IllegalArgumentException> { CodePoint(code) }
            val c = code.toCodePoint()
            assertEquals(code.mod(CodePoint.MAX_VALUE.code + 1), c.code)
        }
    }

    @Test
    fun sizeProperties() {
        for (c in bmpCodePoints) {
            assertEquals(1, c.size)
            assertTrue(c.isBasic)
            assertFalse(c.isSupplementary)
        }

        for (c in supplementaryCodePoints) {
            assertEquals(2, c.size)
            assertFalse(c.isBasic)
            assertTrue(c.isSupplementary)
        }
    }

    @Test
    fun conversions() {
        for (c in bmpCodePoints) {
            assertEquals(c.code.toChar(), c.toSingleChar())

            assertEquals(c, c.toSingleChar().toCodePoint())
            assertEquals(c, CodePoint.fromChar(c.toSingleChar()))

            assertEquals(c.code.toChar().toString(), c.toString())
            assertContentEquals(charArrayOf(c.code.toChar()), c.toCharArray())

            assertFailsWith<IllegalArgumentException> { c.toSurrogatePair() }
            assertFailsWith<IllegalArgumentException> { c.toSurrogatePair { high, low -> high - low } }
        }

        for (c in supplementaryCodePoints) {
            assertFailsWith<IllegalArgumentException> { c.toSingleChar() }

            val pair = c.toSurrogatePair()
            assertEquals(pair, c.toSurrogatePair { high, low -> high to low })
            assertEquals(c, CodePoint.fromSurrogatePair(pair.first, pair.second))
            assertTrue(CodePoint.isSurrogatePair(pair.first, pair.second))

            assertContentEquals(charArrayOf(pair.first, pair.second), c.toCharArray())
            assertEquals(pair.first.toString() + pair.second, c.toString())
        }

        assertFailsWith<IllegalArgumentException> { CodePoint.fromSurrogatePair('a', Char.MIN_LOW_SURROGATE) }
        assertFailsWith<IllegalArgumentException> { CodePoint.fromSurrogatePair(Char.MIN_HIGH_SURROGATE, 'b') }
    }

    @Test
    fun comparison() {
        assertTrue(CodePoint.MIN_VALUE < CodePoint.MAX_VALUE)

        val allCodePoints = bmpCodePoints + supplementaryCodePoints
        for ([index1, c1: CodePoint] in allCodePoints.withIndex()) {
            for ([index2, c2: CodePoint] in allCodePoints.withIndex()) {
                assertEquals((index1 compareTo index2).sign, (c1 compareTo c2).sign)
            }
        }
    }

    @Test
    fun arithmetics() {
        for ([c1, c2, diff] in listOf(
            Triple('a'.toCodePoint(), 'c'.toCodePoint(), 2),
            Triple(0x1F600.toCodePoint(), 0x1FA00.toCodePoint(), 0x400),
            Triple(CodePoint.MAX_VALUE, CodePoint.MIN_VALUE, -0x10FFFF),
        )) {
            assertEquals(c2, c1 + diff)
            assertEquals(c1, c2 - diff)
            assertEquals(diff, c2 - c1)
            assertEquals(-diff, c1 - c2)

            assertEquals(c2, c1 + (diff + (CodePoint.MAX_VALUE.code + 1)))
            assertEquals(c2, c1 + (diff - (CodePoint.MAX_VALUE.code + 1)))
        }

        assertEquals(CodePoint.MIN_VALUE, CodePoint.MAX_VALUE + 1)
        assertEquals(CodePoint.MAX_VALUE, CodePoint.MIN_VALUE - 1)

        var c = CodePoint.MAX_VALUE
        c++
        assertEquals(CodePoint.MIN_VALUE, c)
        c--
        assertEquals(CodePoint.MAX_VALUE, c)
    }

}
