package test.io

import org.junit.Test as test
import kotlin.test.assertEquals
import java.io.File

class FilesTest {
    test fun testCreateTempDir() {
        val dirSuf = System.currentTimeMillis().toString()
        val dir1 = createTempDir("temp", dirSuf)
        assert(dir1.exists() && dir1.isDirectory() && dir1.name.startsWith("temp") && dir1.name.endsWith(dirSuf))
        try {
            createTempDir("a")
            assert(false)
        } catch(e: IllegalArgumentException) {}

        val dir2 = createTempDir("temp")
        assert(dir2.exists() && dir2.isDirectory() && dir2.name.endsWith(".tmp"))

        val dir3 = createTempDir()
        assert(dir3.exists() && dir3.isDirectory())

        dir1.delete()
        dir2.delete()
        dir3.delete()
    }

    test fun listFilesWithFilter() {
        val dir = createTempDir("temp")

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

    test fun relativeTo() {
        assertEquals("kotlin", File("src/kotlin".separatorsToSystem()).relativeTo(File("src")))
        assertEquals("", File("dir").relativeTo(File("dir")))
        assertEquals("..", File("dir").relativeTo(File("dir/subdir".separatorsToSystem())))
        assertEquals("../../test".separatorsToSystem(), File("test").relativeTo(File("dir/dir".separatorsToSystem())))

        val file1 = File("C:/dir1".separatorsToSystem())
        val file2 = File("D:/dir2".separatorsToSystem())
        try {
            val winRelPath = file1.relativeTo(file2)
            assert(file1.canonicalPath.charAt(0) == '/')
            assertEquals("../../C:/dir1", winRelPath)
        } catch (e: IllegalArgumentException) {
            assert(Character.isLetter(file1.canonicalPath.charAt(0)))
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

    test fun extension() {
        assertEquals("bbb", File("aaa.bbb").extension)
        assertEquals("", File("aaa").extension)
        assertEquals("", File("aaa.").extension)
        // maybe we should think that such files have name .bbb and no extension
        assertEquals("bbb", File(".bbb").extension)
    }

    test fun nameWithoutExtension() {
        assertEquals("aaa", File("aaa.bbb").nameWithoutExtension)
        assertEquals("aaa", File("aaa").nameWithoutExtension)
        assertEquals("aaa", File("aaa.").nameWithoutExtension)
        assertEquals("", File(".bbb").nameWithoutExtension)
    }

    test fun separatorsToSystem() {
        var path = "/aaa/bbb/ccc"
        assertEquals(path.replace("/", File.separator), File(path).separatorsToSystem())

        path = "C:\\Program Files\\My Awesome Program"
        assertEquals(path.replace("\\", File.separator), File(path).separatorsToSystem())

        path = "/Libraries\\Java:/Libraries/Python:/Libraries/Ruby"
        assertEquals(path.replace(":", File.pathSeparator), path.pathSeparatorsToSystem())

        path = "/Libraries\\Java;/Libraries/Python;/Libraries/Ruby"
        assertEquals(path.replace(";", File.pathSeparator), path.pathSeparatorsToSystem())

        path = "/Libraries\\Java;/Libraries/Python:\\Libraries/Ruby"
        assertEquals(path.replace("/", File.separator).replace("\\", File.separator)
                .replace(":", File.pathSeparator).replace(";", File.pathSeparator), path.allSeparatorsToSystem())

        assertEquals("test", "test".allSeparatorsToSystem())
    }
}
