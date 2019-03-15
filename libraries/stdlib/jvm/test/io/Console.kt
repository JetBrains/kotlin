/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package test.io

import org.junit.Test
import java.nio.charset.Charset
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
        assertEquals(expected, actual)
        val referenceExpected = readLinesReference(text, charset)
        assertEquals(referenceExpected, actual, "Comparing to reference readLine")

    }

    private fun readLines(text: String, charset: Charset): List<String> {
        text.byteInputStream(charset).use { stream ->
            val decoder = charset.newDecoder()
            @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
            return generateSequence { readLine(stream, decoder) }.toList().also {
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