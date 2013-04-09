package test.collections

import kotlin.test.*

import kotlin.io.*
import kotlin.util.*
import java.io.*
import java.util.*
import org.junit.Test as test

class IoTest(){
    test fun testLineIteratorWithManualClose() {
        val reader = sample().buffered()
        try {
            val list = reader.lineIterator().toArrayList()
            assertEquals(arrayListOf("Hello", "World"), list)
        } finally {
            reader.close()
        }
    }

    fun sample(): Reader {
        return StringReader("Hello\nWorld");
    }

    test fun testLineIterator() {
        // TODO we should maybe zap the useLines approach as it encourages
        // use of iterators which don't close the underlying stream
        val list1 = sample().useLines{ it.toArrayList() }
        val list2 = sample().useLines<ArrayList<String>>{ it.toArrayList() }

        assertEquals(arrayListOf("Hello", "World"), list1)
        assertEquals(arrayListOf("Hello", "World"), list2)
    }

    test fun testForEach() {
        val list = ArrayList<String>()
        val reader = sample().buffered()

        reader.use{
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

    test fun testForEachLine() {
        val list = ArrayList<String>()
        val reader = sample()

        /* TODO would be nicer maybe to write this as
            reader.lines.forEach { ... }

          as we could one day maybe one day write that as
            for (line in reader.lines)

          if the for(elem in thing) {...} statement could act as syntax sugar for
            thing.forEach{ elem -> ... }

          if thing is not an Iterable/array/Iterator but has a suitable forEach method
        */
        reader.forEachLine{
            list.add(it)
        }

        assertEquals(arrayListOf("Hello", "World"), list)
    }

    test fun testForEachLineFile() {
        val file = File.createTempFile("temp", System.nanoTime().toString())
        file.writeText("Hello\nWorld")


        val list = ArrayList<String>()
        file.forEachLine{
            list.add(it)
        }

        assertEquals(arrayListOf("Hello", "World"), list)
        file.deleteOnExit()
    }

    test fun testListFiles() {
        val dir = File.createTempFile("temp", System.nanoTime().toString())
        dir.delete()
        dir.mkdir()

        File.createTempFile("temp", "1.kt", dir)
        File.createTempFile("temp", "2.java", dir)
        File.createTempFile("temp", "3.kt", dir)

        val result = dir.listFiles { it.getName().endsWith(".kt") }

        assertNotNull(result)
        assertEquals(result!!.size, 2)
    }

    test fun relativePath() {
        val file1 = File("src")
        val file2 = File(file1, "kotlin")
        val file3 = File("test")

        assertEquals("kotlin", file1.relativePath(file2))
        assertEquals("", file1.relativePath(file1))
        assertEquals(file3.canonicalPath, file1.relativePath(file3))
    }

    test fun listFiles() {
        val dir = File.createTempFile("temp", System.nanoTime().toString())
        dir.delete()
        dir.mkdir()

        File.createTempFile("temp", "1.kt", dir)
        File.createTempFile("temp", "2.java", dir)

        val subdir = File(dir, "subdir")
        subdir.mkdir()

        File(subdir, "3.txt").createNewFile()

        var totalFiles = 0
        dir.recurse { totalFiles++ }

        assertEquals(5, totalFiles)

        if (subdir.setReadable(false)) {
            // On Windows, we can't make directory not readable, and setReadable() will return false

            var totalFilesWithUnReadableDir = 0
            dir.recurse { totalFilesWithUnReadableDir++ }
            assertEquals(4, totalFilesWithUnReadableDir)
        }
    }

    test fun testAppendText() {
        val file = File.createTempFile("temp", System.nanoTime().toString())
        file.writeText("Hello\n")
        file.appendText("World")

        assertEquals("Hello\nWorld", file.readText())
        file.deleteOnExit()
    }

}
