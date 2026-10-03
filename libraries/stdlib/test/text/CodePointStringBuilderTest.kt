/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*
import kotlin.text.unicode.*


@OptIn(ExperimentalUnicodeApi::class)
class CodePointStringBuilderTest {
    val string = "abc😀def${Char.MIN_HIGH_SURROGATE}-${Char.MIN_LOW_SURROGATE}"

    @Test
    fun appendByOne() {
        val codePoints = string.codePointSequence()

        val results = listOf(
            buildString {
                codePoints.forEach { append(it) }
            },
            buildString {
                codePoints.forEach { appendCodePoint(it) }
            },
            buildString {
                val a = this as Appendable
                codePoints.forEach { a.appendCodePoint(it) }
            },
        )

        results.forEach {
            assertEquals(string, it)
        }
    }

    @Test
    fun insertInReverse() {
        val result = buildString {
            assertFailsWith<IndexOutOfBoundsException> { insertCodePointAt(-1, CodePoint.MAX_VALUE) }
            assertFailsWith<IndexOutOfBoundsException> { insertCodePointAt(1, CodePoint.MAX_VALUE) }
            string.codePointSequence().forEach { insertCodePointAt(0, it) }
        }
        assertEquals(string.reversed(), result)
    }

    @Test
    fun replaceCodePoints() {
        val testData = listOf(
            listOf("ab", 1, "😀".codePointAt(0), "a😀"),
            listOf("${Char.MIN_LOW_SURROGATE}b", 0, "😀".codePointAt(0), "😀b"),
            listOf("😒b", 0, "😀".codePointAt(0), "😀b"),
            listOf("ab", 1, 'c'.toCodePoint(), "ac"),
            listOf("${Char.MIN_HIGH_SURROGATE}b", 0, 'a'.toCodePoint(), "ab"),
        )
        for ([string, pos, value, result] in testData) {
            assertEquals(result, StringBuilder(string as String).apply { setCodePointAt(pos as Int, value as CodePoint) }.toString())
        }
        val _ = buildString {
            assertFailsWith<IndexOutOfBoundsException> { setCodePointAt(0, "😀".codePointAt(0)) }
        }
    }

    @Test
    fun deleteCodePoints() {
        val result = StringBuilder(string).apply {
            assertFailsWith<IndexOutOfBoundsException> { deleteCodePointAt(-1) }
            assertFailsWith<IndexOutOfBoundsException> { deleteCodePointAt(length) }

            repeat(string.codePointSequence().count()) { deleteCodePointAt(0) }
        }
        assertEquals(0, result.length)
    }
}
