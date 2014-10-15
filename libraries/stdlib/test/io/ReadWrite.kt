package test.io

import org.junit.Test as test
import java.io.File
import kotlin.test.assertEquals
import java.io.Reader
import java.io.StringReader
import java.util.ArrayList

fun sample(): Reader = StringReader("Hello\nWorld");

class ReadWriteTest {
    test fun testAppendText() {
        val file = File.createTempFile("temp", System.nanoTime().toString())
        file.writeText("Hello\n")
        file.appendText("World")

        assertEquals("Hello\nWorld", file.readText())
        file.deleteOnExit()
    }

    test fun reader() {
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
        assertEquals(arrayListOf("Hello", "World"), list)

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

    test fun file() {
        val file = File.createTempFile("temp", System.nanoTime().toString())

        file.writeText("Hello\nWorld")
        val list = ArrayList<String>()
        file.forEachLine{
            list.add(it)
        }
        assertEquals(arrayListOf("Hello", "World"), list)

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

    class LineIteratorTest {
        test fun useLines() {
            // TODO we should maybe zap the useLines approach as it encourages
            // use of iterators which don't close the underlying stream
            val list1 = sample().useLines { it.toArrayList() }
            val list2 = sample().useLines<ArrayList<String>>{ it.toArrayList() }

            assertEquals(arrayListOf("Hello", "World"), list1)
            assertEquals(arrayListOf("Hello", "World"), list2)
        }

        test fun manualClose() {
            val reader = sample().buffered()
            try {
                val list = reader.lines().toArrayList()
                assertEquals(arrayListOf("Hello", "World"), list)
            } finally {
                reader.close()
            }
        }

        test fun boundaryConditions() {
            var reader = StringReader("").buffered()
            assertEquals(ArrayList<String>(), reader.lines().toArrayList())
            reader.close()

            reader = StringReader(" ").buffered()
            assertEquals(arrayListOf(" "), reader.lines().toArrayList())
            reader.close()

            reader = StringReader(" \n").buffered()
            assertEquals(arrayListOf(" "), reader.lines().toArrayList())
            reader.close()

            reader = StringReader(" \n ").buffered()
            assertEquals(arrayListOf(" ", " "), reader.lines().toArrayList())
            reader.close()
        }
    }

    test fun testUse() {
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
}
