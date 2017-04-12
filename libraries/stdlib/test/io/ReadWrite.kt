@file:kotlin.jvm.JvmVersion
package test.io

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import java.io.Reader
import java.io.StringReader
import java.net.URL
import java.util.ArrayList
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun sample(): Reader = StringReader("Hello\nWorld");

class ReadWriteTest {
    @Test fun testAppendText() {
        val file = File.createTempFile("temp", System.nanoTime().toString())
        file.writeText("Hello\n")
        file.appendText("World\n")
        file.appendText("Again")

        assertEquals("Hello\nWorld\nAgain", file.readText())
        assertEquals(listOf("Hello", "World", "Again"), file.readLines(Charsets.UTF_8))
        file.deleteOnExit()
    }

    @Test fun reader() {
        val list = ArrayList<String>()

        /* TODO would be nicer maybe to write this as
            reader.lines.forEach { ... }

          as we could one day maybe write that as
            for (line in reader.lines)

          if the for(elem in thing) {...} statement could act as syntax sugar for
            thing.forEach{ elem -> ... }

          if thing is not an Iterable/array/Iterator but has a suitable forEach method
        */
        sample().forEachLine {
            list.add(it)
        }
        assertEquals(listOf("Hello", "World"), list)

        assertEquals(listOf("Hello", "World"), sample().readLines())

        sample().useLines {
            assertEquals(listOf("Hello", "World"), it.toList())
        }


        var reader = StringReader("")
        var c = 0
        reader.forEachLine { c++ }
        assertEquals(0, c)

        reader = StringReader(" ")
        reader.forEachLine { c++ }
        assertEquals(1, c)

        reader = StringReader(" \n")
        c = 0
        reader.forEachLine { c++ }
        assertEquals(1, c)

        reader = StringReader(" \n ")
        c = 0
        reader.forEachLine { c++ }
        assertEquals(2, c)
    }

    @Test fun file() {
        val file = File.createTempFile("temp", System.nanoTime().toString())
        val writer = file.outputStream().writer().buffered()

        writer.write("Hello")
        writer.newLine()
        writer.write("World")
        writer.close()

        //file.replaceText("Hello\nWorld")
        file.forEachBlock { arr: ByteArray, size: Int ->
            assertTrue(size >= 11 && size <= 12, size.toString())
            assertTrue(arr.contains('W'.toByte()))
        }
        val list = ArrayList<String>()
        file.forEachLine(Charsets.UTF_8, {
            list.add(it)
        })
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

        file.deleteOnExit()
    }



    @Test fun testUse() {
        val list = ArrayList<String>()
        val reader = sample().buffered()

        reader.use {
            while (true) {
                val line = it.readLine()
                if (line != null)
                    list.add(line)
                else
                    break
            }
        }

        assertEquals(arrayListOf("Hello", "World"), list)
    }

    @Test fun testPlatformNullUse() {
        fun <T> platformNull() = @Suppress("UNCHECKED_CAST") java.util.Collections.singleton(null as T).first()
        val resource = platformNull<java.io.Closeable>()
        val result = resource.use {
            "ok"
        }
        assertEquals("ok", result)
    }

    @Test fun testURL() {
        val url = URL("http://kotlinlang.org")
        val text = url.readText()
        assertFalse(text.isEmpty())
        val text2 = url.readText(charset("UTF8"))
        assertFalse(text2.isEmpty())
    }
}


class LineIteratorTest {
    @Test fun useLines() {
        // TODO we should maybe zap the useLines approach as it encourages
        // use of iterators which don't close the underlying stream
        val list1 = sample().useLines { it.toList() }
        val list2 = sample().useLines<ArrayList<String>>{ it.toCollection(arrayListOf()) }

        assertEquals(listOf("Hello", "World"), list1)
        assertEquals(listOf("Hello", "World"), list2)
    }

    @Test fun manualClose() {
        val reader = sample().buffered()
        try {
            val list = reader.lineSequence().toList()
            assertEquals(arrayListOf("Hello", "World"), list)
        } finally {
            reader.close()
        }
    }

    @Test fun boundaryConditions() {
        var reader = StringReader("").buffered()
        assertEquals(emptyList(), reader.lineSequence().toList())
        reader.close()

        reader = StringReader(" ").buffered()
        assertEquals(listOf(" "), reader.lineSequence().toList())
        reader.close()

        reader = StringReader(" \n").buffered()
        assertEquals(listOf(" "), reader.lineSequence().toList())
        reader.close()

        reader = StringReader(" \n ").buffered()
        assertEquals(listOf(" ", " "), reader.lineSequence().toList())
        reader.close()
    }
}
