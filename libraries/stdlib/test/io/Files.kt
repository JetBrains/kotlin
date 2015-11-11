package test.io

import java.io.*
import org.junit.Test as test
import kotlin.test.assertEquals
import java.util.NoSuchElementException
import java.util.HashSet
import java.util.ArrayList
import kotlin.io.walkBottomUp
import kotlin.io.walkTopDown
import kotlin.test.*

class FilesTest {

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

    class Walks {

        companion object {
            fun createTestFiles(): File {
                val basedir = createTempDir()
                File(basedir, "1").mkdir()
                File(basedir, "1/2".separatorsToSystem()).mkdir()
                File(basedir, "1/3".separatorsToSystem()).mkdir()
                File(basedir, "1/3/4.txt".separatorsToSystem()).createNewFile()
                File(basedir, "1/3/5.txt".separatorsToSystem()).createNewFile()
                File(basedir, "6").mkdir()
                File(basedir, "7.txt").createNewFile()
                File(basedir, "8").mkdir()
                File(basedir, "8/9.txt".separatorsToSystem()).createNewFile()
                return basedir
            }
        }

        @test fun withSimple() {
            val basedir = createTestFiles()
            try {
                val referenceNames =
                        listOf("", "1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt", "6", "7.txt", "8", "8/9.txt").map(
                                { it -> it.separatorsToSystem() }).toHashSet()
                val namesTopDown = HashSet<String>()
                for (file in basedir.walkTopDown()) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDown.contains(name), "$name is visited twice")
                    namesTopDown.add(name)
                }
                assertEquals(referenceNames, namesTopDown)
                val namesBottomUp = HashSet<String>()
                for (file in basedir.walkBottomUp()) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesBottomUp.contains(name), "$name is visited twice")
                    namesBottomUp.add(name)
                }
                assertEquals(referenceNames, namesBottomUp)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withEnterLeave() {
            val basedir = createTestFiles()
            try {
                val referenceNames =
                        listOf("", "1", "1/2", "1/3", "6", "8").map(
                                { it -> it.separatorsToSystem() }).toHashSet()
                val namesTopDownEnter = HashSet<String>()
                val namesTopDownLeave = HashSet<String>()
                val namesTopDown = HashSet<String>()
                fun enter(file: File) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDownEnter.contains(name), "$name is entered twice")
                    namesTopDownEnter.add(name)
                    assertFalse(namesTopDownLeave.contains(name), "$name is left before entrance")
                }

                fun leave(file: File) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDownLeave.contains(name), "$name is left twice")
                    namesTopDownLeave.add(name)
                    assertTrue(namesTopDownEnter.contains(name), "$name is left before entrance")
                }

                fun visit(file: File) {
                    val name = file.relativeTo(basedir)
                    if (file.isDirectory()) {
                        assertTrue(namesTopDownEnter.contains(name), "$name is visited before entrance")
                        namesTopDown.add(name)
                        assertFalse(namesTopDownLeave.contains(name), "$name is visited after leaving")
                    }
                    if (file == basedir)
                        return
                    val parent = file.getParentFile()
                    if (parent != null) {
                        val parentName = parent.relativeTo(basedir)
                        assertTrue(namesTopDownEnter.contains(parentName),
                                "$name is visited before entering its parent $parentName")
                        assertFalse(namesTopDownLeave.contains(parentName),
                                "$name is visited after leaving its parent $parentName")
                    }
                }
                for (file in basedir.walkTopDown().enter(::enter).leave(::leave)) {
                    visit(file)
                }
                assertEquals(referenceNames, namesTopDownEnter)
                assertEquals(referenceNames, namesTopDownLeave)
                namesTopDownEnter.clear()
                namesTopDownLeave.clear()
                namesTopDown.clear()
                for (file in basedir.walkBottomUp().enter(::enter).leave(::leave)) {
                    visit(file)
                }
                assertEquals(referenceNames, namesTopDownEnter)
                assertEquals(referenceNames, namesTopDownLeave)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withFilterAndMap() {
            val basedir = createTestFiles()
            try {
                val referenceNames =
                        listOf("", "1", "1/2", "1/3", "6", "8").map(
                                { it -> it.separatorsToSystem() }).toHashSet()
                assertEquals(referenceNames, basedir.walkTopDown().filter { it.isDirectory() }.map {
                    it.relativeTo(basedir)
                }.toHashSet())
            } finally {
                basedir.deleteRecursively()
            }

        }

        @test fun withDeleteTxtTopDown() {
            val basedir = createTestFiles()
            try {
                val referenceNames =
                        listOf("", "1", "1/2", "1/3", "6", "8").map(
                                { it -> it.separatorsToSystem() }).toHashSet()
                val namesTopDown = HashSet<String>()
                fun enter(file: File) {
                    assertTrue(file.isDirectory())
                    for (child in file.listFiles()) {
                        if (child.name.endsWith("txt"))
                            child.delete()
                    }
                }
                for (file in basedir.walkTopDown().enter(::enter)) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDown.contains(name), "$name is visited twice")
                    namesTopDown.add(name)
                }
                assertEquals(referenceNames, namesTopDown)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withDeleteTxtBottomUp() {
            val basedir = createTestFiles()
            try {
                val referenceNames =
                        listOf("", "1", "1/2", "1/3", "6", "8").map(
                                { it -> it.separatorsToSystem() }).toHashSet()
                val namesTopDown = HashSet<String>()
                fun enter(file: File) {
                    assertTrue(file.isDirectory())
                    for (child in file.listFiles()) {
                        if (child.name.endsWith("txt"))
                            child.delete()
                    }
                }
                for (file in basedir.walkBottomUp().enter(::enter)) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDown.contains(name), "$name is visited twice")
                    namesTopDown.add(name)
                }
                assertEquals(referenceNames, namesTopDown)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withFilter() {
            val basedir = createTestFiles()
            try {
                fun filter(file: File): Boolean {
                    // Everything ended with 3 is filtered
                    return (!file.name.endsWith("3"));
                }

                val referenceNames =
                        listOf("", "1", "1/2", "6", "7.txt", "8", "8/9.txt").map(
                                { it -> it.separatorsToSystem() }).toHashSet()
                val namesTopDown = HashSet<String>()
                for (file in basedir.walkTopDown().filter(::filter)) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDown.contains(name), "$name is visited twice")
                    namesTopDown.add(name)
                }
                assertEquals(referenceNames, namesTopDown)
                val namesBottomUp = HashSet<String>()
                for (file in basedir.walkBottomUp().filter(::filter)) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesBottomUp.contains(name), "$name is visited twice")
                    namesBottomUp.add(name)
                }
                assertEquals(referenceNames, namesBottomUp)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withTotalFilter() {
            val basedir = createTestFiles()
            try {
                val referenceNames: Set<String> = setOf()
                val namesTopDown = HashSet<String>()
                // Everything is filtered
                for (file in basedir.walkTopDown().filter({ false })) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesTopDown.contains(name), "$name is visited twice")
                    namesTopDown.add(name)
                }
                assertEquals(referenceNames, namesTopDown)
                val namesBottomUp = HashSet<String>()
                // Everything is filtered
                for (file in basedir.walkBottomUp().filter({ false })) {
                    val name = file.relativeTo(basedir)
                    assertFalse(namesBottomUp.contains(name), "$name is visited twice")
                    namesBottomUp.add(name)
                }
                assertEquals(referenceNames, namesBottomUp)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withForEach() {
            val basedir = createTestFiles()
            try {
                var i = 0
                basedir.walkTopDown().forEach { it -> i++ }
                assertEquals(10, i);
                i = 0
                basedir.walkBottomUp().forEach { it -> i++ }
                assertEquals(10, i);
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withCount() {
            val basedir = createTestFiles()
            try {
                assertEquals(10, basedir.walkTopDown().count());
                assertEquals(10, basedir.walkBottomUp().count());
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withReduce() {
            val basedir = createTestFiles()
            try {
                val res = basedir.walkTopDown().reduce { a, b -> if (a.canonicalPath > b.canonicalPath) a else b }
                assertTrue(res.endsWith("9.txt"), "Expected end with 9.txt actual: ${res.name}")
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun withVisitorAndDepth() {
            val basedir = createTestFiles()
            try {
                val files = HashSet<String>()
                val dirs = HashSet<String>()
                val failed = HashSet<String>()
                val stack = ArrayList<File>()
                fun beforeVisitDirectory(dir: File) {
                    stack.add(dir)
                    dirs.add(dir.relativeTo(basedir))
                }

                fun afterVisitDirectory(dir: File) {
                    assertEquals(stack.last(), dir)
                    stack.remove(stack.lastIndex)
                }

                fun visitFile(file: File) {
                    assert(stack.last().listFiles().contains(file)) { file }
                    files.add(file.relativeTo(basedir))
                }

                fun visitDirectoryFailed(dir: File, e: IOException) {
                    assertEquals(stack.last(), dir)
                    stack.remove(stack.lastIndex)
                    failed.add(dir.name)
                }
                basedir.walkTopDown().enter(::beforeVisitDirectory).leave(::afterVisitDirectory).
                        fail(::visitDirectoryFailed).forEach { it -> if (!it.isDirectory()) visitFile(it) }
                assert(stack.isEmpty())
                val sep = File.separator
                for (fileName in arrayOf("", "1", "1${sep}2", "1${sep}3", "6", "8")) {
                    assert(dirs.contains(fileName)) { fileName }
                }
                for (fileName in arrayOf("1${sep}3${sep}4.txt", "1${sep}3${sep}4.txt", "7.txt", "8${sep}9.txt")) {
                    assert(files.contains(fileName)) { fileName }
                }

                //limit maxDepth
                files.clear()
                dirs.clear()
                basedir.walkTopDown().enter(::beforeVisitDirectory).leave(::afterVisitDirectory).maxDepth(1).
                        forEach { it -> if (it != basedir) visitFile(it) }
                assert(stack.isEmpty())
                assert(dirs.size() == 1 && dirs.contains("")) { dirs.size() }
                for (file in arrayOf("1", "6", "7.txt", "8")) {
                    assert(files.contains(file)) { file }
                }

                //restrict access
                if (File(basedir, "1").setReadable(false)) {
                    try {
                        files.clear()
                        dirs.clear()
                        basedir.walkTopDown().enter(::beforeVisitDirectory).leave(::afterVisitDirectory).
                                fail(::visitDirectoryFailed).forEach { it -> if (!it.isDirectory()) visitFile(it) }
                        assert(stack.isEmpty())
                        assert(failed.size() == 1 && failed.contains("1")) { failed.size() }
                        assert(dirs.size() == 4) { dirs.size() }
                        for (dir in arrayOf("", "1", "6", "8")) {
                            assert(dirs.contains(dir)) { dir }
                        }
                        assert(files.size() == 2) { files.size() }
                        for (file in arrayOf("7.txt", "8${sep}9.txt")) {
                            assert(files.contains(file)) { file }
                        }
                    } finally {
                        File(basedir, "1").setReadable(true)
                    }
                } else {
                    System.err.println("cannot restrict access")
                }
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun topDown() {
            val basedir = createTestFiles()
            try {
                val visited = HashSet<File>()
                val block: (File) -> Unit = {
                    assert(!visited.contains(it)) { it }
                    assert(it == basedir && visited.isEmpty() || visited.contains(it.getParentFile())) { it }
                    visited.add(it)
                }
                basedir.walkTopDown().forEach(block)
                assert(visited.size() == 10) { visited.size() }

            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun restrictedAccess() {
            val basedir = createTestFiles()
            val restricted = File(basedir, "1")
            try {
                if (restricted.setReadable(false)) {
                    val visited = HashSet<File>()
                    val block: (File) -> Unit = {
                        assert(!visited.contains(it)) { it }
                        assert(it == basedir && visited.isEmpty() || visited.contains(it.getParentFile())) { it }
                        visited.add(it)
                    }
                    basedir.walkTopDown().forEach(block)
                    assert(visited.size() == 6) { visited.size() }
                }
            } finally {
                restricted.setReadable(true)
                basedir.deleteRecursively()
            }
        }

        @test fun backup() {
            var count = 0
            fun makeBackup(file: File) {
                count++
                val bakFile = File(file.toString() + ".bak")
                file.copyTo(bakFile)
            }

            val basedir1 = createTestFiles()
            try {
                basedir1.walkTopDown().forEach {
                    if (it.isFile()) {
                        makeBackup(it)
                    }
                }
                assert(count == 4)
            } finally {
                basedir1.deleteRecursively()
            }

            count = 0
            val basedir2 = createTestFiles()
            try {
                basedir2.walkTopDown().forEach {
                    if (it.isFile()) {
                        makeBackup(it)
                    }
                }
                assert(count == 4)
            } finally {
                basedir2.deleteRecursively()
            }
        }

        @test fun find() {
            val basedir = createTestFiles()
            try {
                File(basedir, "8/4.txt".separatorsToSystem()).createNewFile()
                var count = 0
                basedir.walkTopDown().takeWhile { it -> count == 0 }.forEach {
                    if (it.name == "4.txt") {
                        count++
                    }
                }
                assert(count == 1)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun findGits() {
            val basedir = createTestFiles()
            try {
                File(basedir, "1/3/.git").mkdir()
                File(basedir, "1/2/.git").mkdir()
                File(basedir, "6/.git").mkdir()
                val found = HashSet<File>()
                for (file in basedir.walkTopDown()) {
                    if (file.name == ".git") {
                        found.add(file.getParentFile())
                    }
                }
                assert(found.size() == 3)
            } finally {
                basedir.deleteRecursively()
            }
        }

        @test fun streamFileTree() {
            val dir = createTempDir()
            try {
                val subDir1 = createTempDir(prefix = "d1_", directory = dir)
                val subDir2 = createTempDir(prefix = "d2_", directory = dir)
                createTempDir(prefix = "d1_", directory = subDir1)
                createTempFile(prefix = "f1_", directory = subDir1)
                createTempDir(prefix = "d1_", directory = subDir2)
                assertEquals(6, dir.walkTopDown().count())
            } finally {
                dir.deleteRecursively()
            }
            dir.mkdir()
            try {
                val it = dir.walkTopDown().iterator()
                it.next()
                it.next()
                assert(false)
            } catch(e: NoSuchElementException) {
            } finally {
                dir.delete()
            }
        }

    }

    @test fun listFilesWithFilter() {
        val dir = createTempDir("temp")

        createTempFile("temp1", ".kt", dir)
        createTempFile("temp2", ".java", dir)
        createTempFile("temp3", ".kt", dir)

        // This line works only with Kotlin File.listFiles(filter)
        val result = dir.listFiles { it.getName().endsWith(".kt") }
        assertEquals(2, result!!.size())
        // This line works both with Kotlin File.listFiles(filter) and the same Java function because of SAM
        val result2 = dir.listFiles { it -> it.getName().endsWith(".kt") }
        assertEquals(2, result2!!.size())
    }

    @test fun relativeToTest() {
        val file1 = File("/foo/bar/baz")
        val file2 = File("/foo/baa/ghoo")
        assertEquals("../../bar/baz".separatorsToSystem(), file1.relativeTo(file2))
        val file3 = File("/foo/bar")
        assertEquals("baz", file1.relativeTo(file3))
        assertEquals("..", file3.relativeTo(file1))
        val file4 = File("/foo/bar/")
        assertEquals("baz", file1.relativeTo(file4))
        assertEquals("..", file4.relativeTo(file1))
        assertEquals("", file3.relativeTo(file4))
        assertEquals("", file4.relativeTo(file3))
        val file5 = File("/foo/baran")
        assertEquals("../bar".separatorsToSystem(), file3.relativeTo(file5))
        assertEquals("../baran".separatorsToSystem(), file5.relativeTo(file3))
        assertEquals("../bar".separatorsToSystem(), file4.relativeTo(file5))
        assertEquals("../baran".separatorsToSystem(), file5.relativeTo(file4))
        val file6 = File("C:\\Users\\Me")
        val file7 = File("C:\\Users\\Me\\Documents")
        assertEquals("..", file6.relativeTo(file7))
        assertEquals("Documents", file7.relativeTo(file6))
        val file8 = File("//my.host/home/user/documents/vip")
        val file9 = File("//my.host/home/other/images/nice")
        assertEquals("../../../user/documents/vip".separatorsToSystem(), file8.relativeTo(file9))
        assertEquals("../../../other/images/nice".separatorsToSystem(), file9.relativeTo(file8))
        val file10 = File("foo/bar")
        val file11 = File("foo")
        assertEquals("bar", file10.relativeTo(file11))
        assertEquals("..", file11.relativeTo(file10))
    }

    @test fun relativeTo() {
        assertEquals("kotlin", File("src/kotlin".separatorsToSystem()).relativeTo(File("src")))
        assertEquals("", File("dir").relativeTo(File("dir")))
        assertEquals("..", File("dir").relativeTo(File("dir/subdir".separatorsToSystem())))
        assertEquals("../../test".separatorsToSystem(), File("test").relativeTo(File("dir/dir".separatorsToSystem())))

        // This test operates correctly only at Windows PCs with C & D drives
        val file1 = File("C:/dir1".separatorsToSystem())
        val file2 = File("D:/dir2".separatorsToSystem())
        try {
            file1.relativeTo(file2)
            assert(false);
        } catch (e: IllegalArgumentException) {
            // It's the thing we should get here
        } catch (e: IOException) {
            // The device is not ready (D) ==> DO NOTHING
        }
    }

    @test fun relativePath() {
        val file1 = File("src")
        val file2 = File(file1, "kotlin")
        val file3 = File("test")

        assertEquals("kotlin", file1.relativePath(file2))
        assertEquals("", file1.relativePath(file1))
        assertEquals(file3.canonicalPath, file1.relativePath(file3))
    }

    private fun checkFileElements(f: File, root: File?, elements: List<String>) {
        var i = 0
        assertEquals(root, f.root)
        for (elem in f.filePathComponents().fileList) {
            assertTrue(i < elements.size(), i.toString())
            assertEquals(elements[i++], elem.toString())
        }
        assertEquals(elements.size(), i)
    }

    @test fun fileIterator() {
        checkFileElements(File("/foo/bar"), File("/"), listOf("foo", "bar"))
        checkFileElements(File("\\foo\\bar"), File("\\".separatorsToSystem()), listOf("foo", "bar"))
        checkFileElements(File("/foo/bar/gav"), File("/"), listOf("foo", "bar", "gav"))
        checkFileElements(File("/foo/bar/gav/"), File("/"), listOf("foo", "bar", "gav"))
        checkFileElements(File("bar/gav"), null, listOf("bar", "gav"))
        checkFileElements(File("C:\\bar\\gav"), File("C:\\".separatorsToSystem()), listOf("bar", "gav"))
        checkFileElements(File("C:/bar/gav"), File("C:/"), listOf("bar", "gav"))
        checkFileElements(File("C:\\"), File("C:\\".separatorsToSystem()), listOf())
        checkFileElements(File("C:/"), File("C:/"), listOf())
        checkFileElements(File("C:"), File("C:"), listOf())
        if (File.separatorChar == '\\') {
            // Check only in Windows
            checkFileElements(File("\\\\host.ru\\home\\mike"), File("\\\\host.ru\\home"), listOf("mike"))
            checkFileElements(File("//host.ru/home/mike"), File("//host.ru/home"), listOf("mike"))
        }
        checkFileElements(File(""), null, listOf(""))
        checkFileElements(File("."), null, listOf("."))
        checkFileElements(File(".."), null, listOf(".."))
    }

    @test fun startsWith() {
        assertTrue(File("C:\\Users\\Me\\Temp\\Game").startsWith("C:\\Users\\Me"))
        assertFalse(File("C:\\Users\\Me\\Temp\\Game").startsWith("C:\\Users\\He"))
        assertTrue(File("C:\\Users\\Me").startsWith("C:\\"))
    }

    @test fun endsWith() {
        assertTrue(File("/foo/bar").endsWith("bar"))
        assertTrue(File("/foo/bar").endsWith("/bar"))
        assertTrue(File("/foo/bar/gav/bar").endsWith("/bar"))
        assertTrue(File("/foo/bar/gav/bar").endsWith("/gav/bar"))
        assertFalse(File("/foo/bar/gav").endsWith("/bar"))
        assertFalse(File("foo/bar").endsWith("/bar"))
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
        assertEquals(File("C:\\windows").separatorsToSystem(),
                File("C:\\home\\..\\documents\\..\\windows").normalize().separatorsToSystem())
        assertEquals(File("C:/windows"), File("C:/home/../documents/../windows").normalize())
        assertEquals(File("foo"), File("gav/bar/../../foo").normalize())
    }

    @test fun resolve() {
        assertEquals(File("/foo/bar/gav"), File("/foo/bar").resolve("gav"))
        assertEquals(File("/foo/bar/gav"), File("/foo/bar/").resolve("gav"))
        assertEquals(File("/gav"), File("/foo/bar").resolve("/gav"))
        // For Unix C:\path is not correct so it's cannot be automatically converted
        assertEquals(File("C:\\Users\\Me\\Documents\\important.doc").separatorsToSystem(),
                File("C:\\Users\\Me").resolve("Documents\\important.doc").separatorsToSystem())
        assertEquals(File("C:/Users/Me/Documents/important.doc"),
                File("C:/Users/Me").resolve("Documents/important.doc"))
    }

    @test fun resolveSibling() {
        assertEquals(File("/foo/gav"), File("/foo/bar").resolveSibling("gav"))
        assertEquals(File("/foo/gav"), File("/foo/bar/").resolveSibling("gav"))
        assertEquals(File("/gav"), File("/foo/bar").resolveSibling("/gav"))
        // For Unix C:\path is not correct so it's cannot be automatically converted
        assertEquals(File("C:\\Users\\Me\\Documents\\important.doc").separatorsToSystem(),
                File("C:\\Users\\Me\\profile.ini").resolveSibling("Documents\\important.doc").separatorsToSystem())
        assertEquals(File("C:/Users/Me/Documents/important.doc"),
                File("C:/Users/Me/profile.ini").resolveSibling("Documents/important.doc"))
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
            println(e.getMessage())
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
        val basedir = Walks.createTestFiles()
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
