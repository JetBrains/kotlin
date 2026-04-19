/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.text.codepoints.*
import kotlin.test.*

@OptIn(ExperimentalCodePointApi::class)
class CodePointIterationTest {

    @Test
    fun codePointAtBefore() {
        fun <T> testCodePointAtBefore(
            s: (String) -> T,
            size: T.() -> Int,
            codePointAt: T.(Int) -> CodePoint,
            codePointBefore: T.(Int) -> CodePoint,
        ) {
            val string = s("abc😀def")
            val unpairedSurrogates = s("a${Char.MIN_HIGH_SURROGATE}b${Char.MIN_LOW_SURROGATE}")

            run {
                val c2 = string.codePointAt(2)
                val c3 = string.codePointAt(3)
                val c4 = string.codePointAt(4)
                assertEquals('c', c2.toSingleChar())
                assertEquals(0x1F600, c3.code)
                val (high, low) = c3.toSurrogatePair()
                assertEquals(low, c4.toSingleChar())

                assertFailsWith<IndexOutOfBoundsException> { string.codePointAt(-1) }
                assertFailsWith<IndexOutOfBoundsException> { string.codePointAt(string.size()) }

                assertEquals(Char.MIN_HIGH_SURROGATE, unpairedSurrogates.codePointAt(1).toSingleChar())
                assertEquals(Char.MIN_LOW_SURROGATE, unpairedSurrogates.codePointAt(3).toSingleChar())
            }

            run {
                val c2 = string.codePointBefore(3)
                val c3 = string.codePointBefore(4)
                val c4 = string.codePointBefore(5)
                assertEquals('c', c2.toSingleChar())
                assertEquals(0x1F600, c4.code)
                val (high, low) = c4.toSurrogatePair()
                assertEquals(high, c3.toSingleChar())
                assertEquals('f', string.codePointBefore(string.size()).toSingleChar())

                assertFailsWith<IndexOutOfBoundsException> { string.codePointBefore(0) }
                assertFailsWith<IndexOutOfBoundsException> { string.codePointBefore(string.size() + 1) }

                assertEquals(Char.MIN_HIGH_SURROGATE, unpairedSurrogates.codePointBefore(2).toSingleChar())
                assertEquals(Char.MIN_LOW_SURROGATE, unpairedSurrogates.codePointBefore(4).toSingleChar())
            }
        }

        testCodePointAtBefore({ it }, String::length, String::codePointAt, String::codePointBefore)
        testCodePointAtBefore({ it as CharSequence }, CharSequence::length, CharSequence::codePointAt, CharSequence::codePointBefore)
        testCodePointAtBefore({ StringBuilder(it) }, CharSequence::length, CharSequence::codePointAt, CharSequence::codePointBefore)
        testCodePointAtBefore({ it.toCharArray() }, CharArray::size, CharArray::codePointAt, CharArray::codePointBefore)
    }

    @Test
    fun iteration() {
        fun String.codePointSequences(): List<CodePointSequence> = listOf(
            this.codePointSequence(),
            (this as CharSequence).codePointSequence(),
            StringBuilder(this).codePointSequence(),
            this.toCharArray().codePointSequence(),
        )

        val string = "abc😀def"
        for (seq in string.codePointSequences()) {
            assertEquals(listOf("a", "b", "c", "😀", "d", "e", "f"), seq.map { it.toString() }.toList())
        }

        val unpairedSurrogates = "a${Char.MIN_HIGH_SURROGATE}b${Char.MIN_LOW_SURROGATE}"
        for (seq in unpairedSurrogates.codePointSequences()) {
            assertEquals(unpairedSurrogates.toList(), seq.map { it.toSingleChar() }.toList())
        }
    }
}
