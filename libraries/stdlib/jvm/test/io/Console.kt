/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package test.io

import org.junit.Test
import java.nio.charset.Charset
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

class ConsoleTest {
    private val linuxLineSeparator: String = "\n"
    private val windowsLineSeparator: String = "\r\n"

    @Test
    fun shouldReadEmptyLine() {
        testReadLine("", emptyList())
    }

    @Test
    fun shouldReadSingleLine() {
        for (length in 1..3) {
            val line = buildString { repeat(length) { append('a' + it) } }
            testReadLine(line, listOf(line))
        }
    }

    @Test
    fun trailingEmptyLineIsIgnored() {
        testReadLine(linuxLineSeparator, listOf(""))
        testReadLine(windowsLineSeparator, listOf(""))
        testReadLine("a$linuxLineSeparator", listOf("a"))
        testReadLine("a$windowsLineSeparator", listOf("a"))
    }

    @Test
    fun shouldReadOneLine() {
        testReadLine("first", listOf("first"))
    }

    @Test
    fun shouldReadTwoLines() {
        testReadLine("first${linuxLineSeparator}second", listOf("first", "second"))
    }

    @Test
    fun shouldReadMultipleEmptyLines() {
        testReadLine(
            "first${linuxLineSeparator}second${linuxLineSeparator}${linuxLineSeparator}${linuxLineSeparator}",
            listOf("first", "second", "", "")
        )
    }

    @Test
    fun shouldReadAloneCarriageReturn() {
        val result = readLines("\r", Charsets.UTF_8)
        assertEquals(listOf("\r"), result)
    }

    @Test
    fun shouldReadConsecutiveEmptyLines() {
        testReadLine("$linuxLineSeparator$linuxLineSeparator", listOf("", ""))
        testReadLine("$linuxLineSeparator$windowsLineSeparator", listOf("", ""))
        testReadLine("$windowsLineSeparator$linuxLineSeparator", listOf("", ""))
        testReadLine("$windowsLineSeparator$windowsLineSeparator", listOf("", ""))
    }

    @Test
    fun shouldReadWindowsLineSeparator() {
        testReadLine("first${windowsLineSeparator}second", listOf("first", "second"))
    }

    @Test
    fun shouldReadMultibyteEncodings() {
        testReadLine("first${linuxLineSeparator}second", listOf("first", "second"), charset = Charsets.UTF_32)
    }

    @Test
    fun shouldReadAllSupportedEncodings() {
        val lines = listOf(
            "ONE", "TWICE", "", "0123456", 
            "This is a very long line that will overflow buffers that are allocated in the code of LineReader object",
            "This line is quite short",
            "x".repeat(1000), // stress
            "7", "8", "9" // some short stuff at the end
        )
        // Filter all available charsets that can be encoded
        val charsets: List<Charset> = Charset.availableCharsets().values.filter { charset ->
            try {
                charset.newEncoder()
                true // take it
            } catch (e: UnsupportedOperationException) {
                false // we can only test charset that supports encoding, skip it
            }
        }
        // Run the test
        for (separator in listOf(linuxLineSeparator, windowsLineSeparator)) {
            val text = lines.joinToString(separator)
            for (charset in charsets) {
                val reference = readLinesReference(text, charset)
                if (reference != lines) continue // this encoding does not support ASCII chars that we test, skip
                // Now we can test readLine function
                val actual = readLines(text, charset)
                assertEquals(lines, actual, "Comparing with $charset")
            }
        }
    }

    @Test
    fun shouldReadAllUnicodeCodePoints() {
        // Generate lines of ever-increasing length with sample unicode code points to stress all corner-cases in
        // line lengths and ability to handle different bit-lengths of unicode code points.
        var cp = 0
        val rnd = Random(1)
        val logFactor = 7 // log of number of code points that are sampled per each bit length of code point
        fun nextCP(): Int {
            if (cp == 10 || cp == 13) cp++ // skip line endings
            if (cp in 0xD800..0xFFFF) cp = 0x10000 // skip surrogates
            // to make the test run faster don't test all code points, the larger they are, the sparser they are sampled
            // For each bit length of the code point we randomly sample ~2^logFactor code points for this test
            val maxStep = cp.coerceAtLeast(1 shl logFactor).takeHighestOneBit() shr logFactor
            val step = rnd.nextInt(1..maxStep)
            return cp.also { cp += step }
        }
        val lines = ArrayList<String>().apply {
            var len = 1
            while (cp < Character.MAX_CODE_POINT) {
                add(buildString {
                    repeat(len) {
                        appendCodePoint(nextCP())
                        if (cp >= Character.MAX_CODE_POINT) return@buildString
                    }
                })
                len++
            }
        }
        // test all standard unicode encoding that should be able to represent all code points
        for (separator in listOf(linuxLineSeparator, windowsLineSeparator)) {
            val text = lines.joinToString(separator)
            for (charset in listOf(Charsets.UTF_8, Charsets.UTF_16BE, Charsets.UTF_16LE, Charsets.UTF_32BE, Charsets.UTF_32LE)) {
                testReadLine(text, lines, charset)
            }
        }
    }

    @Test
    fun readSurrogatePairs() {
        val c = "\uD83D\uDC4D" // thumb-up emoji
        testReadLine("$c$linuxLineSeparator", listOf(c))
        testReadLine("e $c$linuxLineSeparator", listOf("e $c"))
        testReadLine("$c$windowsLineSeparator", listOf(c))
        testReadLine("e $c$c", listOf("e $c$c"))
        testReadLine("e $c$linuxLineSeparator$c", listOf("e $c", c))
    }

    private fun testReadLine(text: String, expected: List<String>, charset: Charset = Charsets.UTF_8) {
        val actual = readLines(text, charset)
        assertEquals(expected, actual, "Comparing with $charset")
        val referenceExpected = readLinesReference(text, charset)
        assertEquals(referenceExpected, actual, "Comparing to reference readLine")
    }

    private fun readLines(text: String, charset: Charset): List<String> {
        text.byteInputStream(charset).use { stream ->
            @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
            return generateSequence { LineReader.readLine(stream, charset) }.toList().also {
                assertTrue("All bytes should be read") { stream.read() == -1 }
            }
        }
    }

    private fun readLinesReference(text: String, charset: Charset): List<String> {
        text.byteInputStream(charset).bufferedReader(charset).use { reader ->
            return generateSequence { reader.readLine() }.toList()
        }
    }
}