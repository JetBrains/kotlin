package test.io

import java.io.*
import org.junit.Test as test
import kotlin.io.walkTopDown
import kotlin.test.*

class FilesTest {

    private val isCaseInsensitiveFileSystem = File("C:/") == File("c:/")

    @test fun testPath() {
        val fileSuf = System.currentTimeMillis().toString()
        val file1 = createTempFile("temp", fileSuf)
        assertTrue(file1.path.endsWith(fileSuf), file1.path)
    }

    @test fun testCreateTempDir() {
        val dirSuf = System.currentTimeMillis().toString()
        val dir1 = createTempDir("temp", dirSuf)
        assert(dir1.exists() && dir1.isDirectory() && dir1.name.startsWith("temp") && dir1.name.endsWith(dirSuf))
        try {
            createTempDir("a")
            assert(false)
        } catch(e: IllegalArgumentException) {
        }

        val dir2 = createTempDir("temp")
        assert(dir2.exists() && dir2.isDirectory() && dir2.name.endsWith(".tmp"))

        val dir3 = createTempDir()
        assert(dir3.exists() && dir3.isDirectory())

        dir1.delete()
        dir2.delete()
        dir3.delete()
    }

    @test fun testCreateTempFile() {
        val fileSuf = System.currentTimeMillis().toString()
        val file1 = createTempFile("temp", fileSuf)
        assert(file1.exists() && file1.name.startsWith("temp") && file1.name.endsWith(fileSuf))
        try {
            createTempFile("a")
            assert(false)
        } catch(e: IllegalArgumentException) {
        }
        val file2 = createTempFile("temp")
        assert(file2.exists() && file2.name.endsWith(".tmp"))

        val file3 = createTempFile()
        assert(file3.exists())

        file1.delete()
        file2.delete()
        file3.delete()
    }

    @test fun listFilesWithFilter() {
        val dir = createTempDir("temp")

        createTempFile("temp1", ".kt", dir)
        createTempFile("temp2", ".java", dir)
        createTempFile("temp3", ".kt", dir)

        // This line works only with Kotlin File.listFiles(filter)
        val result = dir.listFiles { it.getName().endsWith(".kt") }
        assertEquals(2, result!!.size)
        // This line works both with Kotlin File.listFiles(filter) and the same Java function because of SAM
        val result2 = dir.listFiles { it -> it.getName().endsWith(".kt") }
        assertEquals(2, result2!!.size)
    }

    @test fun relativeToRooted() {
        val file1 = File("/foo/bar/baz")
        val file2 = File("/foo/baa/ghoo")

        assertEquals("../../bar/baz", file1.relativeToFile(file2).invariantSeparatorsPath)

        val file3 = File("/foo/bar")

        assertEquals("baz", file1.relativeTo(file3))
        assertEquals("..", file3.relativeTo(file1))

        val file4 = File("/foo/bar/")

        assertEquals("baz", file1.relativeTo(file4))
        assertEquals("..", file4.relativeTo(file1))
        assertEquals("", file3.relativeTo(file4))
        assertEquals("", file4.relativeTo(file3))

        val file5 = File("/foo/baran")

        assertEquals("../bar", file3.relativeToFile(file5).invariantSeparatorsPath)
        assertEquals("../baran", file5.relativeToFile(file3).invariantSeparatorsPath)
        assertEquals("../bar", file4.relativeToFile(file5).invariantSeparatorsPath)
        assertEquals("../baran", file5.relativeToFile(file4).invariantSeparatorsPath)

        val file6 = File("C:\\Users\\Me")
        val file7 = File("C:\\Users\\Me\\Documents")

        assertEquals("..", file6.relativeTo(file7))
        assertEquals("Documents", file7.relativeTo(file6))

        if (isCaseInsensitiveFileSystem) {
            assertEquals("bar", File("C:/bar").relativeTo(File("c:/")))
        }

        val file8 = File("""\\my.host\home/user/documents/vip""")
        val file9 = File("""\\my.host\home/other/images/nice""")

        assertEquals("../../../user/documents/vip", file8.relativeToFile(file9).invariantSeparatorsPath)
        assertEquals("../../../other/images/nice", file9.relativeToFile(file8).invariantSeparatorsPath)
    }

    @test fun relativeToRelative() {
        val nested = File("foo/bar")
        val base = File("foo")

        assertEquals("bar", nested.relativeTo(base))
        assertEquals("..", base.relativeTo(nested))

        val empty = File("")
        val current = File(".")
        val parent = File("..")
        val outOfRoot = File("../bar")

        assertEquals(File("../bar"), File(outOfRoot.relativeTo(empty)))
        assertEquals(File("../../bar"), File(outOfRoot.relativeTo(base)))
        assertEquals("bar", outOfRoot.relativeTo(parent))
        assertEquals("..", parent.relativeTo(outOfRoot))

        val root = File("/root")
        val files = listOf(nested, base, empty, outOfRoot, current, parent)
        val bases = listOf(nested, base, empty, current)

        for (file in files)
            assertEquals("", file.relativeTo(file), "file should have empty path relative to itself: $file")

        for (file in files) {
            for (base in bases) {
                val rootedFile = root.resolve(file)
                val rootedBase = root.resolve(base)
                assertEquals(file.relativeTo(base), rootedFile.relativeTo(rootedBase), "nested: $file, base: $base")
            }
        }
    }

    @test fun relativeToFails() {
        val absolute = File("/foo/bar/baz")
        val relative = File("foo/bar")
        val networkShare1 = File("""\\my.host\share1/folder""")
        val networkShare2 = File("""\\my.host\share2/folder""")

        fun assertFailsRelativeTo(file: File, base: File) {
            val e = assertFailsWith<IllegalArgumentException>("file: $file, base: $base") { file.relativeTo(base) }
            println(e.message)
        }

        val allFiles = listOf(absolute, relative, networkShare1, networkShare2)
        for (file in allFiles) {
            for (base in allFiles) {
                if (file != base) assertFailsRelativeTo(file, base)
            }
        }

        assertFailsRelativeTo(File("y"), File("../x"))

        // This test operates correctly only at Windows PCs with C & D drives
        val fileOnC = File("C:/dir1")
        val fileOnD = File("D:/dir2")
        assertFailsRelativeTo(fileOnC, fileOnD)
    }

    @test fun relativeTo() {
        assertEquals("kotlin", File("src/kotlin").relativeTo(File("src")))
        assertEquals("", File("dir").relativeTo(File("dir")))
        assertEquals("..", File("dir").relativeTo(File("dir/subdir")))
        assertEquals(File("../../test"), File("test").relativeToFile(File("dir/dir")))
    }

    private fun checkFilePathComponents(f: File, root: File, elements: List<String>) {
        assertEquals(root, f.root)
        val components = f.toComponents()
        assertEquals(root, components.root)
        assertEquals(elements, components.segments.map { it.toString() })
    }

    @test fun filePathComponents() {
        checkFilePathComponents(File("/foo/bar"), File("/"), listOf("foo", "bar"))
        checkFilePathComponents(File("/foo/bar/gav"), File("/"), listOf("foo", "bar", "gav"))
        checkFilePathComponents(File("/foo/bar/gav/"), File("/"), listOf("foo", "bar", "gav"))
        checkFilePathComponents(File("bar/gav"), File(""), listOf("bar", "gav"))
        checkFilePathComponents(File("C:/bar/gav"), File("C:/"), listOf("bar", "gav"))
        checkFilePathComponents(File("C:/"), File("C:/"), listOf())
        checkFilePathComponents(File("C:"), File("C:"), listOf())
        if (File.separator == "\\") {
            // Check only in Windows
            checkFilePathComponents(File("\\\\host.ru\\home\\mike"), File("\\\\host.ru\\home"), listOf("mike"))
            checkFilePathComponents(File("//host.ru/home/mike"), File("//host.ru/home"), listOf("mike"))
            checkFilePathComponents(File("\\foo\\bar"), File("\\"), listOf("foo", "bar"))
            checkFilePathComponents(File("C:\\bar\\gav"), File("C:\\"), listOf("bar", "gav"))
            checkFilePathComponents(File("C:\\"), File("C:\\"), listOf())
        }
        checkFilePathComponents(File(""), File(""), listOf())
        checkFilePathComponents(File("."), File(""), listOf("."))
        checkFilePathComponents(File(".."), File(""), listOf(".."))
    }

    @test fun fileRoot() {
        val rooted = File("/foo/bar")
        assertTrue(rooted.isRooted)
        assertEquals("/", rooted.root.invariantSeparatorsPath)

        if (File.separator == "\\") {
            val diskRooted = File("""C:\foo\bar""")
            assertTrue(rooted.isRooted)
            assertEquals("""C:\""", diskRooted.rootName)

            val networkRooted = File("""\\network\share\""")
            assertTrue(networkRooted.isRooted)
            assertEquals("""\\network\share""", networkRooted.rootName)
        }

        val relative = File("foo/bar")
        assertFalse(relative.isRooted)
        assertEquals("", relative.rootName)
    }

    @test fun startsWith() {
        assertTrue(File("C:\\Users\\Me\\Temp\\Game").startsWith("C:\\Users\\Me"))
        assertFalse(File("C:\\Users\\Me\\Temp\\Game").startsWith("C:\\Users\\He"))
        assertTrue(File("C:\\Users\\Me").startsWith("C:\\"))
        if (isCaseInsensitiveFileSystem) {
            assertTrue(File("C:\\Users\\Me").startsWith("c:\\"))
        }
    }

    @test fun endsWith() {
        assertTrue(File("/foo/bar").endsWith("bar"))
        assertTrue(File("/foo/bar").endsWith("/bar"))
        assertTrue(File("/foo/bar/gav/bar").endsWith("/bar"))
        assertTrue(File("/foo/bar/gav/bar").endsWith("/gav/bar"))
        assertFalse(File("/foo/bar/gav").endsWith("/bar"))
        assertFalse(File("foo/bar").endsWith("/bar"))
        if (isCaseInsensitiveFileSystem) {
            assertTrue(File("/foo/bar").endsWith("Bar"))
        }
    }

    @test fun subPath() {
        if (File.separatorChar == '\\') {
            // Check only in Windows
            assertEquals(File("mike"), File("//my.host.net/home/mike/temp").subPath(0, 1))
            assertEquals(File("mike"), File("\\\\my.host.net\\home\\mike\\temp").subPath(0, 1))
        }
        assertEquals(File("bar/gav"), File("/foo/bar/gav/hi").subPath(1, 3))
        assertEquals(File("foo"), File("/foo/bar/gav/hi").subPath(0, 1))
        assertEquals(File("gav/hi"), File("/foo/bar/gav/hi").subPath(2, 4))
    }

    @test fun normalize() {
        assertEquals(File("/foo/bar/baaz"), File("/foo/./bar/gav/../baaz").normalize())
        assertEquals(File("/foo/bar/baaz"), File("/foo/bak/../bar/gav/../baaz").normalize())
        assertEquals(File("../../bar"), File("../foo/../../bar").normalize())
        // For Unix C:\windows is not correct so it's not the same as C:/windows
        if (File.separator == "\\") {
            assertEquals(File("C:\\windows"), File("C:\\home\\..\\documents\\..\\windows").normalize())
            assertEquals(File("C:/windows"), File("C:/home/../documents/../windows").normalize())
        }
        assertEquals(File("foo"), File("gav/bar/../../foo").normalize())
        assertEquals(File("/../foo"), File("/bar/../../foo").normalize())
    }

    @test fun resolve() {
        assertEquals(File("/foo/bar/gav"), File("/foo/bar").resolve("gav"))
        assertEquals(File("/foo/bar/gav"), File("/foo/bar/").resolve("gav"))
        assertEquals(File("/gav"), File("/foo/bar").resolve("/gav"))
        // For Unix C:\path is not correct so it's cannot be automatically converted
        if (File.separator == "\\") {
            assertEquals(File("C:\\Users\\Me\\Documents\\important.doc"),
                    File("C:\\Users\\Me").resolve("Documents\\important.doc"))
            assertEquals(File("C:/Users/Me/Documents/important.doc"),
                    File("C:/Users/Me").resolve("Documents/important.doc"))
        }
        assertEquals(File(""), File("").resolve(""))
        assertEquals(File("bar"), File("").resolve("bar"))
        assertEquals(File("foo/bar"), File("foo").resolve("bar"))
        // should it normalize such paths?
//        assertEquals(File("bar"), File("foo").resolve("../bar"))
//        assertEquals(File("../bar"), File("foo").resolve("../../bar"))
//        assertEquals(File("foo/bar"), File("foo").resolve("./bar"))
    }

    @test fun resolveSibling() {
        assertEquals(File("/foo/gav"), File("/foo/bar").resolveSibling("gav"))
        assertEquals(File("/foo/gav"), File("/foo/bar/").resolveSibling("gav"))
        assertEquals(File("/gav"), File("/foo/bar").resolveSibling("/gav"))
        // For Unix C:\path is not correct so it's cannot be automatically converted
        if (File.separator == "\\") {
            assertEquals(File("C:\\Users\\Me\\Documents\\important.doc"),
                    File("C:\\Users\\Me\\profile.ini").resolveSibling("Documents\\important.doc"))
            assertEquals(File("C:/Users/Me/Documents/important.doc"),
                    File("C:/Users/Me/profile.ini").resolveSibling("Documents/important.doc"))
        }
        assertEquals(File("gav"), File("foo").resolveSibling("gav"))
        assertEquals(File("../gav"), File("").resolveSibling("gav"))
    }

    @test fun extension() {
        assertEquals("bbb", File("aaa.bbb").extension)
        assertEquals("", File("aaa").extension)
        assertEquals("", File("aaa.").extension)
        // maybe we should think that such files have name .bbb and no extension
        assertEquals("bbb", File(".bbb").extension)
        assertEquals("", File("/my.dir/log").extension)
    }

    @test fun nameWithoutExtension() {
        assertEquals("aaa", File("aaa.bbb").nameWithoutExtension)
        assertEquals("aaa", File("aaa").nameWithoutExtension)
        assertEquals("aaa", File("aaa.").nameWithoutExtension)
        assertEquals("", File(".bbb").nameWithoutExtension)
        assertEquals("log", File("/my.dir/log").nameWithoutExtension)
    }

    @test fun separatorsToSystem() {
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

    @test fun testCopyTo() {
        val srcFile = createTempFile()
        val dstFile = createTempFile()
        srcFile.writeText("Hello, World!", "UTF8")
        try {
            srcFile.copyTo(dstFile)
            assert(false)
        } catch (e: FileAlreadyExistsException) {
            println(e.message)
        }

        var len = srcFile.copyTo(dstFile, overwrite = true)
        assertEquals(13L, len)
        assertEquals(srcFile.readText(), dstFile.readText("UTF8"))

        assert(dstFile.delete())
        len = srcFile.copyTo(dstFile)
        assertEquals(13L, len)
        assertEquals(srcFile.readText("UTF8"), dstFile.readText())

        assert(dstFile.delete())
        dstFile.mkdir()
        val child = File(dstFile, "child")
        child.createNewFile()
        srcFile.copyTo(dstFile, overwrite = true)
        assertEquals(13L, len)
        val copy = dstFile.resolve(srcFile.name)
        assertEquals(srcFile.readText(), copy.readText())

        assert(srcFile.delete())
        assert(child.delete() && copy.delete() && dstFile.delete())

        try {
            srcFile.copyTo(dstFile)
            assert(false)
        } catch (e: NoSuchFileException) {
        }

        srcFile.mkdir()
        try {
            srcFile.copyTo(dstFile)
            assert(false)
        } catch (e: IllegalArgumentException) {
        }
        srcFile.delete()
    }

    @test fun copyToNameWithoutParent() {
        val currentDir = File("").getAbsoluteFile()!!
        val srcFile = createTempFile()
        val dstFile = createTempFile(directory = currentDir)
        try {
            srcFile.writeText("Hello, World!", "UTF8")
            dstFile.delete()

            val dstRelative = File(dstFile.name)

            srcFile.copyTo(dstRelative)

            assertEquals(srcFile.readText(), dstFile.readText())
        }
        finally {
            dstFile.delete()
            srcFile.delete()
        }
    }

    @test fun deleteRecursively() {
        val dir = createTempDir()
        dir.delete()
        dir.mkdir()
        val subDir = File(dir, "subdir");
        subDir.mkdir()
        File(dir, "test1.txt").createNewFile()
        File(subDir, "test2.txt").createNewFile()

        assert(dir.deleteRecursively())
        assert(!dir.exists())
        assert(!dir.deleteRecursively())
    }

    @test fun deleteRecursivelyWithFail() {
        val basedir = FileTreeWalkTest.createTestFiles()
        val restricted = File(basedir, "1")
        try {
            if (restricted.setReadable(false)) {
                if (File(basedir, "7.txt").setReadable(false)) {
                    basedir.deleteRecursively()
                    restricted.setReadable(true)
                    File(basedir, "7.txt").setReadable(true)
                    var i = 0
                    for (file in basedir.walkTopDown()) {
                        i++
                    }
                    assertEquals(6, i)
                }
            }
        } finally {
            restricted.setReadable(true)
            File(basedir, "7.txt").setReadable(true)
            basedir.deleteRecursively()
        }
    }

    @test fun copyRecursively() {
        val src = createTempDir()
        val dst = createTempDir()
        dst.delete()
        fun check() {
            for (file in src.walkTopDown()) {
                val dstFile = File(dst, file.relativeTo(src))
                assert(dstFile.exists())
                if (dstFile.isFile()) {
                    assertEquals(file.readText(), dstFile.readText())
                }

            }
        }

        try {
            val subDir1 = createTempDir(prefix = "d1_", directory = src)
            val subDir2 = createTempDir(prefix = "d2_", directory = src)
            createTempDir(prefix = "d1_", directory = subDir1)
            val file1 = createTempFile(prefix = "f1_", directory = src)
            val file2 = createTempFile(prefix = "f2_", directory = subDir1)
            file1.writeText("hello")
            file2.writeText("wazzup")
            createTempDir(prefix = "d1_", directory = subDir2)

            assert(src.copyRecursively(dst))
            check()

            try {
                src.copyRecursively(dst)
                assert(false)
            } catch (e: FileAlreadyExistsException) {
            }

            var conflicts = 0
            src.copyRecursively(dst) {
                file: File, e: IOException ->
                if (e is FileAlreadyExistsException) {
                    conflicts++
                    OnErrorAction.SKIP
                } else {
                    throw e
                }
            }
            assert(conflicts == 2)

            if (subDir1.setReadable(false)) {
                try {
                    dst.deleteRecursively()
                    var caught = false
                    assert(src.copyRecursively(dst) {
                        file: File, e: IOException ->
                        if (e is AccessDeniedException) {
                            caught = true
                            OnErrorAction.SKIP
                        } else {
                            throw e
                        }
                    })
                    assert(caught)
                    check()
                } finally {
                    subDir1.setReadable(true)
                }
            }

            src.deleteRecursively()
            dst.deleteRecursively()
            try {
                src.copyRecursively(dst)
                assert(false)
            } catch (e: NoSuchFileException) {
            }

            assert(!src.copyRecursively(dst) {
                file: File, e: IOException ->
                OnErrorAction.TERMINATE
            })
        } finally {
            src.deleteRecursively()
            dst.deleteRecursively()
        }
    }

    @test fun helpers1() {
        val str = "123456789\n"
        System.setIn(str.byteInputStream())
        val reader = System.`in`.bufferedReader()
        assertEquals("123456789", reader.readLine())
        val stringReader = str.reader()
        assertEquals('1', stringReader.read().toChar())
        assertEquals('2', stringReader.read().toChar())
        assertEquals('3', stringReader.read().toChar())
    }

    @test fun helpers2() {
        val file = createTempFile()
        val writer = file.printWriter()
        val str1 = "Hello, world!"
        val str2 = "Everything is wonderful!"
        writer.println(str1)
        writer.println(str2)
        writer.close()
        val reader = file.bufferedReader()
        assertEquals(str1, reader.readLine())
        assertEquals(str2, reader.readLine())
    }
}
