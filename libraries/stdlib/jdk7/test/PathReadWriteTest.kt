/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.*
import kotlin.test.*

class PathReadWriteTest {
    @Test
    fun testAppendText() {
        val file = Files.createTempFile(null, null)
        file.writeText("Hello\n")
        file.appendText("World\n")
        file.writeText("Again", Charsets.US_ASCII, StandardOpenOption.APPEND)

        assertEquals("Hello\nWorld\nAgain", file.readText())
        assertEquals(listOf("Hello", "World", "Again"), file.readLines(Charsets.UTF_8))
        file.toFile().deleteOnExit()
    }

    @Test
    fun file() {
        val file = Files.createTempFile(null, null)
        val writer = file.outputStream().writer().buffered()

        writer.write("Hello")
        writer.newLine()
        writer.write("World")
        writer.close()

        val list = ArrayList<String>()
        file.forEachLine(charset = Charsets.UTF_8, options = arrayOf(StandardOpenOption.READ)) {
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

        file.toFile().deleteOnExit()
    }

    @Test
    fun testBufferedReader() {
        val file = Files.createTempFile(null, null)
        val lines = listOf("line1", "line2")
        Files.write(file, lines, Charsets.UTF_8)

        assertEquals(file.bufferedReader().use { it.readLines() }, lines)
        assertEquals(file.bufferedReader(Charsets.UTF_8, 1024, StandardOpenOption.READ).use { it.readLines() }, lines)
    }

    @Test
    fun testBufferedWriter() {
        val file = Files.createTempFile(null, null)

        file.bufferedWriter().use { it.write("line1\n") }
        file.bufferedWriter(Charsets.UTF_8, 1024, StandardOpenOption.APPEND).use { it.write("line2\n") }

        assertEquals(Files.readAllLines(file, Charsets.UTF_8), listOf("line1", "line2"))
    }

    @Test
    fun testPrintWriter() {
        val file = Files.createTempFile(null, null)

        val writer = file.printWriter()
        val str1 = "Hello, world!"
        val str2 = "Everything is wonderful!"
        writer.println(str1)
        writer.println(str2)
        writer.close()

        val writer2 = file.printWriter(options = arrayOf(StandardOpenOption.APPEND))
        val str3 = "Hello again!"
        writer2.println(str3)
        writer2.close()

        val writer3 = file.printWriter(Charsets.UTF_8, StandardOpenOption.APPEND)
        val str4 = "Hello one last time!"
        writer3.println(str4)
        writer3.close()

        file.bufferedReader().use { reader ->
            assertEquals(str1, reader.readLine())
            assertEquals(str2, reader.readLine())
            assertEquals(str3, reader.readLine())
            assertEquals(str4, reader.readLine())
        }
    }

    @Test
    fun testWriteBytes() {
        val file = Files.createTempFile(null, null)
        file.writeBytes("Hello".encodeToByteArray())
        file.appendBytes(" world!".encodeToByteArray())
        assertEquals(file.readText(), "Hello world!")
    }
}

