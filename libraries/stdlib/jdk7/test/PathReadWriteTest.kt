/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.nio.file.StandardOpenOption
import kotlin.io.path.*
import kotlin.random.Random
import kotlin.test.*

class PathReadWriteTest : AbstractPathTest() {
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

