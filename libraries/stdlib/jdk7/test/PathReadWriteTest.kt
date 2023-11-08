/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.ByteArrayOutputStream
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*
import kotlin.random.Random
import kotlin.test.*

class PathReadWriteTest : AbstractPathTest() {

    private fun String.encodeToByteArray(charset: Charset): ByteArray {
        val out = ByteArrayOutputStream()
        out.writer(charset).use { it.append(this) }
        return out.toByteArray()
    }

    private val hexFormat = HexFormat {
        bytes.bytesPerLine = 32
        bytes.bytesPerGroup = 8
    }

    private fun Path.testContentEquals(expectedContent: ByteArray, charset: Charset) {
        val expected = expectedContent.toHexString(hexFormat)
        val actualContent = readBytes()
        val actual = actualContent.toHexString(hexFormat)
        assertEquals(expected, actual, "$charset. Expected size is ${expectedContent.size}, actual size is ${actualContent.size}")
    }

    private fun Path.testWriteText(text: String, charset: Charset) {
        // Path.writeText
        val encodedText = text.encodeToByteArray(charset)

        writeText(text, charset)
        testContentEquals(encodedText, charset)

        writeText(StringBuilder(text), charset)
        testContentEquals(encodedText, charset)

        val position = 1.coerceAtMost(text.length)
        val limit = (text.length - 1).coerceAtLeast(position)
        val charBuffer = CharBuffer.wrap(text, position, limit)
        val encodedCharBuffer = text.substring(position, limit).encodeToByteArray(charset)

        writeText(charBuffer, charset)
        testContentEquals(encodedCharBuffer, charset)
        assertEquals(position, charBuffer.position())
        assertEquals(limit, charBuffer.limit())
        assertEquals(text.length, charBuffer.capacity())

        // Path.appendText
        val prefix = "_"
        val encodedPrefix = prefix.encodeToByteArray(charset)

        writeText(prefix, charset)
        appendText(text, charset)
        testContentEquals(encodedPrefix + encodedText, charset)

        writeText(prefix, charset)
        appendText(StringBuilder(text), charset)
        testContentEquals(encodedPrefix + encodedText, charset)

        writeText(prefix, charset)
        appendText(charBuffer, charset)
        testContentEquals(encodedPrefix + encodedCharBuffer, charset)
        assertEquals(position, charBuffer.position())
        assertEquals(limit, charBuffer.limit())
        assertEquals(text.length, charBuffer.capacity())

        // File.writeText
        toFile().writeText(text, charset)
        testContentEquals(encodedText, charset)

        // File.appendText
        toFile().writeText(prefix, charset)
        toFile().appendText(text, charset)
        testContentEquals(encodedPrefix + encodedText, charset)
    }

    @Test fun writeText() {
        val charsets = listOf(
            Charsets.UTF_8,
            Charsets.UTF_16,
            Charsets.UTF_32,
            Charsets.ISO_8859_1,
            Charsets.US_ASCII,
        )

        val highSurrogate = Char.MIN_HIGH_SURROGATE
        val lowSurrogate = Char.MIN_LOW_SURROGATE

        val smallString = "Hello"

        val chunkSize = DEFAULT_BUFFER_SIZE
        val string = "k".repeat(chunkSize - 1)

        val path = createTempFile().cleanup()

        for (charset in charsets) {
            path.testWriteText("$highSurrogate", charset)

            path.testWriteText("$lowSurrogate", charset)

            path.testWriteText("$smallString$highSurrogate", charset)

            path.testWriteText("$smallString$lowSurrogate", charset)

            path.testWriteText("$string$highSurrogate", charset)

            path.testWriteText("$string$lowSurrogate", charset)

            path.testWriteText("$string$highSurrogate$lowSurrogate$string", charset)

            path.testWriteText("$string$lowSurrogate$highSurrogate$string", charset)

            path.testWriteText(
                "$string$highSurrogate$lowSurrogate${string.substring(2)}$highSurrogate$lowSurrogate",
                charset
            )

            path.testWriteText("$string$lowSurrogate$highSurrogate$lowSurrogate$string", charset)
        }
    }

    @Test
    fun appendText() {
        val file = createTempFile().cleanup()
        file.writeText("Hello\n")
        file.appendText("World\n" as CharSequence)
        file.writeText(StringBuilder("Again"), Charsets.US_ASCII, StandardOpenOption.APPEND)

        assertEquals("Hello\nWorld\nAgain", file.readText())
        assertEquals(listOf("Hello", "World", "Again"), file.readLines(Charsets.UTF_8))
    }

    @Test
    fun file() {
        val file = createTempFile().cleanup()
        val writer = file.outputStream().writer().buffered()

        writer.write("Hello")
        writer.newLine()
        writer.write("World")
        writer.close()

        val list = ArrayList<String>()
        file.forEachLine(charset = Charsets.UTF_8) {
            list.add(it)
        }
        assertEquals(listOf("Hello", "World"), list)

        assertEquals(listOf("Hello", "World"), file.readLines())

        file.useLines {
            assertEquals(listOf("Hello", "World"), it.toList())
        }

        val text = file.inputStream().reader().readText()
        assertTrue("Hello" in text)
        assertTrue("World" in text)

        file.writeText("")
        var c = 0
        file.forEachLine { c++ }
        assertEquals(0, c)

        file.writeText(" ")
        file.forEachLine { c++ }
        assertEquals(1, c)

        file.writeText(" \n")
        c = 0
        file.forEachLine { c++ }
        assertEquals(1, c)

        file.writeText(" \n ")
        c = 0
        file.forEachLine { c++ }
        assertEquals(2, c)
    }

    @Test
    fun bufferedReader() {
        val file = createTempFile().cleanup()
        val lines = listOf("line1", "line2")
        file.writeLines(lines)

        assertEquals(lines, file.bufferedReader().use { it.readLines() })
        assertEquals(lines, file.bufferedReader(Charsets.UTF_8, 1024, StandardOpenOption.READ).use { it.readLines() })
    }

    @Test
    fun bufferedWriter() {
        val file = createTempFile().cleanup()

        file.bufferedWriter().use { it.write("line1\n") }
        file.bufferedWriter(Charsets.UTF_8, 1024, StandardOpenOption.APPEND).use { it.write("line2\n") }

        assertEquals(listOf("line1", "line2"), file.readLines())
    }

    @Test
    fun writeBytes() {
        val file = createTempFile().cleanup()
        file.writeBytes("Hello".encodeToByteArray())
        file.appendBytes(" world!".encodeToByteArray())
        assertEquals(file.readText(), "Hello world!")

        val bytes = Random.nextBytes(100)
        file.writeBytes(bytes)
        file.appendBytes(bytes)
        assertTrue((bytes + bytes) contentEquals file.readBytes())
    }

    @Test
    fun writeLines() {
        val file = createTempFile().cleanup()
        val lines = listOf("first line", "second line")
        file.writeLines(lines)
        assertEquals(lines, file.readLines())

        file.writeLines(lines.asSequence())
        assertEquals(lines, file.readLines())

        val moreLines = listOf("third line", "the bottom line")
        file.appendLines(moreLines)
        assertEquals(lines + moreLines, file.readLines())

        file.appendLines(moreLines.asSequence())
        assertEquals(lines + moreLines + moreLines, file.readLines())
    }
}

