package test.io

import org.junit.Test as test
import java.io.File
import kotlin.test.assertEquals

class FilesTest {
    test fun listFilesWithFilter() {
        val dir = File.createTempFile("temp", System.nanoTime().toString())
        dir.delete()
        dir.mkdir()

        File.createTempFile("temp1", ".kt", dir)
        File.createTempFile("temp2", ".java", dir)
        File.createTempFile("temp3", ".kt", dir)

        val result = dir.listFiles { it.getName().endsWith(".kt") }
        assertEquals(2, result!!.size())
    }

    test fun recurse() {
        val dir = File.createTempFile("temp", System.nanoTime().toString())
        dir.delete()

        var totalFiles = 0
        dir.recurse { totalFiles++ }
        assertEquals(1, totalFiles)

        dir.mkdir()

        File.createTempFile("temp", "1.kt", dir)
        File.createTempFile("temp", "2.java", dir)

        val subdir = File(dir, "subdir")
        subdir.mkdir()

        File(subdir, "3.txt").createNewFile()

        totalFiles = 0
        dir.recurse { totalFiles++ }

        assertEquals(5, totalFiles)

        if (subdir.setReadable(false)) {
            // On Windows, we can't make directory not readable, and setReadable() will return false

            totalFiles = 0
            dir.recurse { totalFiles++ }
            assertEquals(4, totalFiles)
        }
    }

    test fun relativePath() {
        val file1 = File("src")
        val file2 = File(file1, "kotlin")
        val file3 = File("test")

        assertEquals("kotlin", file1.relativePath(file2))
        assertEquals("", file1.relativePath(file1))
        assertEquals(file3.canonicalPath, file1.relativePath(file3))
    }
}
