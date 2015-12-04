package test.io

import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.test.*

class FileTreeWalkTest {

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

    @Test fun withSimple() {
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

    @Test fun singleFile() {
        val testFile = createTempFile()
        val nonExistantFile = testFile.resolve("foo")
        try {
            for (walk in listOf(File::walkTopDown, File::walkBottomUp)) {
                assertEquals(testFile, walk(testFile).single(), "${walk.name}")
                assertTrue(walk(testFile).treeFilter { false }.none(), "${walk.name}")
                assertEquals(testFile, testFile.walk().onEnter { false }.single(), "${walk.name} - enter should not be called for single file")

                assertTrue(walk(nonExistantFile).none(), "${walk.name} - enter should not be called for single file")
            }
        }
        finally {
            testFile.delete()
        }
    }

    @Test fun withEnterLeave() {
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

    @Test fun withFilterAndMap() {
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

    @Test fun withDeleteTxtTopDown() {
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

    @Test fun withDeleteTxtBottomUp() {
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

    private fun compareWalkResults(expected: Set<String>, basedir: File, filter: (File) -> Boolean) {
        val namesTopDown = HashSet<String>()
        for (file in basedir.walkTopDown().treeFilter { filter(it) }) {
            val name = file.relativeTo(basedir)
            assertFalse(namesTopDown.contains(name), "$name is visited twice")
            namesTopDown.add(name)
        }
        assertEquals(expected, namesTopDown, "Top-down walk results differ")
        val namesBottomUp = HashSet<String>()
        for (file in basedir.walkBottomUp().treeFilter { filter(it) }) {
            val name = file.relativeTo(basedir)
            assertFalse(namesBottomUp.contains(name), "$name is visited twice")
            namesBottomUp.add(name)
        }
        assertEquals(expected, namesBottomUp, "Bottom-up walk results differ")
    }

    @Test fun withFilter() {
        val basedir = createTestFiles()
        try {
            // Every directory ended with 3 and its content is filtered out
            fun filter(file: File): Boolean = !file.name.endsWith("3")

            val referenceNames = listOf("", "1", "1/2", "6", "7.txt", "8", "8/9.txt").map { File(it).path }.toSet()
            compareWalkResults(referenceNames, basedir, ::filter)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withTotalFilter() {
        val basedir = createTestFiles()
        try {
            val referenceNames = emptySet<String>()
            compareWalkResults(referenceNames, basedir, { false })
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withForEach() {
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

    @Test fun withCount() {
        val basedir = createTestFiles()
        try {
            assertEquals(10, basedir.walkTopDown().count());
            assertEquals(10, basedir.walkBottomUp().count());
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withReduce() {
        val basedir = createTestFiles()
        try {
            val res = basedir.walkTopDown().reduce { a, b -> if (a.canonicalPath > b.canonicalPath) a else b }
            assertTrue(res.endsWith("9.txt"), "Expected end with 9.txt actual: ${res.name}")
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withVisitorAndDepth() {
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
                stack.removeAt(stack.lastIndex)
            }

            fun visitFile(file: File) {
                assert(stack.last().listFiles().contains(file)) { file }
                files.add(file.relativeTo(basedir))
            }

            fun visitDirectoryFailed(dir: File, e: IOException) {
                assertEquals(stack.last(), dir)
                //stack.removeAt(stack.lastIndex)
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
            assert(dirs.size == 1 && dirs.contains("")) { dirs.size }
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
                    assert(failed.size == 1 && failed.contains("1")) { failed.size }
                    assert(dirs.size == 4) { dirs.size }
                    for (dir in arrayOf("", "1", "6", "8")) {
                        assert(dirs.contains(dir)) { dir }
                    }
                    assert(files.size == 2) { files.size }
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

    @Test fun topDown() {
        val basedir = createTestFiles()
        try {
            val visited = HashSet<File>()
            val block: (File) -> Unit = {
                assert(!visited.contains(it)) { it }
                assert(it == basedir && visited.isEmpty() || visited.contains(it.getParentFile())) { it }
                visited.add(it)
            }
            basedir.walkTopDown().forEach(block)
            assert(visited.size == 10) { visited.size }

        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun restrictedAccess() {
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
                assert(visited.size == 6) { visited.size }
            }
        } finally {
            restricted.setReadable(true)
            basedir.deleteRecursively()
        }
    }

    @Test fun backup() {
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

    @Test fun find() {
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

    @Test fun findGits() {
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
            assert(found.size == 3)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun streamFileTree() {
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
