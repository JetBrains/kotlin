/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io

import org.junit.Test
import java.nio.charset.Charset
import kotlin.test.*

class ConsoleTest {
    private val defaultLineSeparator: String = "\\n"

    @Test
    fun shouldReadEmptyLine() {
        testReadLine("")
    }

    @Test
    fun shouldReadOneLetter() {
        testReadLine("a", "a")
    }

    @Test
    fun shouldReadOneLine() {
        testReadLine("first", "first")
    }

    @Test
    fun shouldReadTwoLines() {
        testReadLine("first${defaultLineSeparator}second", "first", "second")
    }

    @Test
    fun shouldReadWindowsLineSeparator() {
        val lineSeparator = "\\r\\n"
        testReadLine("first${lineSeparator}second", "first", "second", lineSeparator = lineSeparator)
    }

    @Test
    fun shouldReadOldMacLineSeparator() {
        val lineSeparator = "\\r"
        testReadLine("first${lineSeparator}second", "first", "second", lineSeparator = lineSeparator)
    }

    @Test
    fun shouldReadMultibyteEncodings() {
        testReadLine("first${defaultLineSeparator}second", "first", "second", charset = Charsets.UTF_32)
    }

    private fun testReadLine(
            text: String,
            vararg expected: String,
            lineSeparator: String = defaultLineSeparator,
            charset: Charset = Charsets.UTF_8
    ) {
        val actual = readLines(text, lineSeparator, charset)
        assertEquals(expected.asList(), actual)
    }

    private fun readLines(text: String, lineSeparator: String, charset: Charset): List<String> {
        text.byteInputStream(charset).use { stream ->
            val decoder = charset.newDecoder()
            return generateSequence { readLine(stream, lineSeparator, decoder) }.toList().also {
                assertTrue("All bytes should be read") { stream.read() == -1 }
            }
        }
    }
}