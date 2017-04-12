@file:kotlin.jvm.JvmVersion
package test.io

import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.test.*

class FileTreeWalkTest {

    companion object {
        val referenceFilenames =
                listOf("1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt", "6", "7.txt", "8", "8/9.txt")
        fun createTestFiles(): File {
            val basedir = createTempDir()
            for (name in referenceFilenames) {
                val file = basedir.resolve(name)
                if (file.extension.isEmpty())
                    file.mkdir()
                else
                    file.createNewFile()
            }
            return basedir
        }
    }

    @Test fun withSimple() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("") + referenceFilenames
            val namesTopDown = HashSet<String>()
            for (file in basedir.walkTopDown()) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                assertFalse(namesTopDown.contains(name), "$name is visited twice")
                namesTopDown.add(name)
            }
            assertEquals(referenceNames, namesTopDown)
            val namesBottomUp = HashSet<String>()
            for (file in basedir.walkBottomUp()) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
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
                    setOf("", "1", "1/2", "6", "8")
            val namesTopDownEnter = HashSet<String>()
            val namesTopDownLeave = HashSet<String>()
            val namesTopDown = HashSet<String>()
            fun enter(file: File): Boolean {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                assertTrue(file.isDirectory, "$name is not directory, only directories should be entered")
                assertFalse(namesTopDownEnter.contains(name), "$name is entered twice")
                assertFalse(namesTopDownLeave.contains(name), "$name is left before entrance")
                if (file.name == "3") return false // filter out 3
                namesTopDownEnter.add(name)
                return true
            }

            fun leave(file: File) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                assertTrue(file.isDirectory, "$name is not directory, only directories should be left")
                assertFalse(namesTopDownLeave.contains(name), "$name is left twice")
                namesTopDownLeave.add(name)
                assertTrue(namesTopDownEnter.contains(name), "$name is left before entrance")
            }

            fun visit(file: File) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                if (file.isDirectory) {
                    assertTrue(namesTopDownEnter.contains(name), "$name is visited before entrance")
                    namesTopDown.add(name)
                    assertFalse(namesTopDownLeave.contains(name), "$name is visited after leaving")
                }
                if (file == basedir)
                    return
                val parent = file.parentFile
                if (parent != null) {
                    val parentName = parent.relativeToOrSelf(basedir).invariantSeparatorsPath
                    assertTrue(namesTopDownEnter.contains(parentName),
                            "$name is visited before entering its parent $parentName")
                    assertFalse(namesTopDownLeave.contains(parentName),
                            "$name is visited after leaving its parent $parentName")
                }
            }
            for (file in basedir.walkTopDown().onEnter(::enter).onLeave(::leave)) {
                visit(file)
            }
            assertEquals(referenceNames, namesTopDownEnter)
            assertEquals(referenceNames, namesTopDownLeave)
            namesTopDownEnter.clear()
            namesTopDownLeave.clear()
            namesTopDown.clear()
            for (file in basedir.walkBottomUp().onEnter(::enter).onLeave(::leave)) {
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
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            assertEquals(referenceNames, basedir.walkTopDown().filter { it.isDirectory }.map {
                it.relativeToOrSelf(basedir).invariantSeparatorsPath
            }.toHashSet())
        } finally {
            basedir.deleteRecursively()
        }

    }

    @Test fun withDeleteTxtTopDown() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            val namesTopDown = HashSet<String>()
            fun enter(file: File) {
                assertTrue(file.isDirectory)
                for (child in file.listFiles()) {
                    if (child.name.endsWith("txt"))
                        child.delete()
                }
            }
            for (file in basedir.walkTopDown().onEnter { enter(it); true }) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
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
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            val namesTopDown = HashSet<String>()
            fun enter(file: File) {
                assertTrue(file.isDirectory)
                for (child in file.listFiles()) {
                    if (child.name.endsWith("txt"))
                        child.delete()
                }
            }
            for (file in basedir.walkBottomUp().onEnter { enter(it); true }) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
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
        for (file in basedir.walkTopDown().onEnter { filter(it) }) {
            val name = file.toRelativeString(basedir)
            assertFalse(namesTopDown.contains(name), "$name is visited twice")
            namesTopDown.add(name)
        }
        assertEquals(expected, namesTopDown, "Top-down walk results differ")
        val namesBottomUp = HashSet<String>()
        for (file in basedir.walkBottomUp().onEnter { filter(it) }) {
            val name = file.toRelativeString(basedir)
            assertFalse(namesBottomUp.contains(name), "$name is visited twice")
            namesBottomUp.add(name)
        }
        assertEquals(expected, namesBottomUp, "Bottom-up walk results differ")
    }

    @Test fun withDirectoryFilter() {
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

    @Test fun withTotalDirectoryFilter() {
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
            basedir.walkTopDown().forEach { _ -> i++ }
            assertEquals(10, i);
            i = 0
            basedir.walkBottomUp().forEach { _ -> i++ }
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
            val files = HashSet<File>()
            val dirs = HashSet<File>()
            val failed = HashSet<String>()
            val stack = ArrayList<File>()
            fun beforeVisitDirectory(dir: File): Boolean {
                stack.add(dir)
                dirs.add(dir.relativeToOrSelf(basedir))
                return true
            }

            fun afterVisitDirectory(dir: File) {
                assertEquals(stack.last(), dir)
                stack.removeAt(stack.lastIndex)
            }

            fun visitFile(file: File) {
                assertTrue(stack.last().listFiles().contains(file), file.toString())
                files.add(file.relativeToOrSelf(basedir))
            }

            fun visitDirectoryFailed(dir: File, @Suppress("UNUSED_PARAMETER") e: IOException) {
                assertEquals(stack.last(), dir)
                //stack.removeAt(stack.lastIndex)
                failed.add(dir.name)
            }
            basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).
                    onFail(::visitDirectoryFailed).forEach { it -> if (!it.isDirectory) visitFile(it) }
            assertTrue(stack.isEmpty())
            for (fileName in arrayOf("", "1", "1/2", "1/3", "6", "8")) {
                assertTrue(dirs.contains(File(fileName)), fileName)
            }
            for (fileName in arrayOf("1/3/4.txt", "1/3/4.txt", "7.txt", "8/9.txt")) {
                assertTrue(files.contains(File(fileName)), fileName)
            }

            //limit maxDepth
            files.clear()
            dirs.clear()
            basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).maxDepth(1).
                    forEach { it -> if (it != basedir) visitFile(it) }
            assertTrue(stack.isEmpty())
            assertEquals(setOf(File("")), dirs)
            for (file in arrayOf("1", "6", "7.txt", "8")) {
                assertTrue(files.contains(File(file)), file.toString())
            }

            //restrict access
            if (File(basedir, "1").setReadable(false)) {
                try {
                    files.clear()
                    dirs.clear()
                    basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).
                            onFail(::visitDirectoryFailed).forEach { it -> if (!it.isDirectory) visitFile(it) }
                    assertTrue(stack.isEmpty())
                    assertEquals(setOf("1"), failed)
                    assertEquals(listOf("", "1", "6", "8").map { File(it) }.toSet(), dirs)
                    assertEquals(listOf("7.txt", "8/9.txt").map { File(it) }.toSet(), files)
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
                assertTrue(!visited.contains(it), it.toString())
                assertTrue(it == basedir && visited.isEmpty() || visited.contains(it.parentFile), it.toString())
                visited.add(it)
            }
            basedir.walkTopDown().forEach(block)
            assertEquals(10, visited.size)
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
                    assertTrue(!visited.contains(it), it.toString())
                    assertTrue(it == basedir && visited.isEmpty() || visited.contains(it.parentFile), it.toString())
                    visited.add(it)
                }
                basedir.walkTopDown().forEach(block)
                assertEquals(6, visited.size)
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
                if (it.isFile) {
                    makeBackup(it)
                }
            }
            assertEquals(4, count)
        } finally {
            basedir1.deleteRecursively()
        }

        count = 0
        val basedir2 = createTestFiles()
        try {
            basedir2.walkTopDown().forEach {
                if (it.isFile) {
                    makeBackup(it)
                }
            }
            assertEquals(4, count)
        } finally {
            basedir2.deleteRecursively()
        }
    }

    @Test fun find() {
        val basedir = createTestFiles()
        try {
            File(basedir, "8/4.txt").createNewFile()
            var count = 0
            basedir.walkTopDown().takeWhile { _ -> count == 0 }.forEach {
                if (it.name == "4.txt") {
                    count++
                }
            }
            assertEquals(1, count)
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
                    found.add(file.parentFile)
                }
            }
            assertEquals(3, found.size)
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
            assertFailsWith<NoSuchElementException>("Second call to next() should fail.") { it.next() }
        } finally {
            dir.delete()
        }
    }

}
