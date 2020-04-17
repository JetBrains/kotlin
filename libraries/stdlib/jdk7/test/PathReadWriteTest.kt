/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathReadWriteTest {
    @Test
    fun testAppendText() {
        val file = Files.createTempFile(null, null)
        file.writeText("Hello\n")
        file.appendText("World\n")
        file.appendText("Again")

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

        file.forEachBlock { arr: ByteArray, size: Int ->
            assertTrue(size in 11..12, size.toString())
            assertTrue(arr.contains('W'.toByte()))
        }
        val list = ArrayList<String>()
        file.forEachLine(StandardOpenOption.READ, charset = Charsets.UTF_8) {
            list.add(it)
        }
        assertEquals(arrayListOf("Hello", "World"), list)

        assertEquals(arrayListOf("Hello", "World"), file.readLines())

        file.useLines {
            assertEquals(arrayListOf("Hello", "World"), it.toList())
        }

        val text = file.inputStream().reader().readText()
        assertTrue(text.contains("Hello"))
        assertTrue(text.contains("World"))

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
}

