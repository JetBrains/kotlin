/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.io

import java.io.File
import java.io.IOException
import java.util.*
import kotlin.test.*

class FileTreeWalkTest {

    companion object {
        val referenceFilenames =
            listOf("1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt", "6", "7.txt", "8", "8/9.txt")

        fun createTestFiles(): File = createTestFiles(referenceFilenames)

        fun createTestFiles(references: List<String>): File {
            val basedir = @Suppress("DEPRECATION") createTempDir()
            for (name in references) {
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
        val testFile = @Suppress("DEPRECATION") createTempFile()
        val nonExistentFile = testFile.resolve("foo")
        try {
            for (walk in listOf(File::walkTopDown, File::walkBottomUp, File::walkBreadthFirst)) {
                assertEquals(testFile, walk(testFile).single(), walk.name)
                assertEquals(testFile, testFile.walk().onEnter { false }.single(), "${walk.name} - enter should not be called for single file")

                assertTrue(walk(nonExistentFile).none(), "${walk.name} - enter should not be called for single file")
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

    @Test fun withEnterLeaveBreadthFirst() {
        val basedir = createTestFiles()
        try {
            val referenceNames =
                setOf("", "1", "1/2", "6", "8")
            val entered = HashSet<String>()
            val left = HashSet<String>()
            val visited = HashSet<String>()
            fun enter(file: File): Boolean {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                assertTrue(file.isDirectory, "$name is not directory, only directories should be entered")
                assertFalse(entered.contains(name), "$name is entered twice")
                assertFalse(left.contains(name), "$name is left before entrance")
                assertTrue(visited.contains(name), "$name entered before visiting")
                if (file.name == "3") return false // filter out 3
                entered.add(name)
                return true
            }

            fun leave(file: File) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                assertTrue(file.isDirectory, "$name is not directory, only directories should be left")
                assertFalse(left.contains(name), "$name is left twice")
                left.add(name)
                assertTrue(entered.contains(name), "$name is left before entrance")
            }

            fun visit(file: File) {
                val name = file.relativeToOrSelf(basedir).invariantSeparatorsPath
                if (file.isDirectory) {
                    visited.add(name)
                    assertFalse(left.contains(name), "$name is visited after leaving")
                }
                if (file == basedir)
                    return
                val parent = file.parentFile
                if (parent != null) {
                    val parentName = parent.relativeToOrSelf(basedir).invariantSeparatorsPath
                    assertTrue(
                        entered.contains(parentName),
                        "$name is visited before entering its parent $parentName"
                    )
                    assertFalse(
                        left.contains(parentName),
                        "$name is visited after leaving its parent $parentName"
                    )
                }
            }
            for (file in basedir.walkBreadthFirst().onEnter(::enter).onLeave(::leave)) {
                visit(file)
            }
            assertEquals(referenceNames, entered)
            assertEquals(referenceNames, left)
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

    @Test fun withFilterAndMapBreadthFirst() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            assertEquals(referenceNames, basedir.walkBreadthFirst().filter { it.isDirectory }.map {
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

    @Test fun withDeleteTxtBreadthFirst() {
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
            for (file in basedir.walkBreadthFirst().onEnter { enter(it); true }) {
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

    @Test fun withDirectoryFilterBreadthFirst() {
        val basedir = createTestFiles()
        try {
            // Every directory ended with 3 and its content is filtered out
            fun filter(file: File) = !file.name.endsWith("3")
            // expect to visit 1/3 but not go inside it
            val visitedExpected = listOf("", "1", "1/2", "1/3", "6", "7.txt", "8", "8/9.txt").map { File(it).path }.toSet()
            val visited = HashSet<String>()
            for (file in basedir.walkBreadthFirst().onEnter { filter(it) }) {
                val name = file.toRelativeString(basedir)
                assertFalse(visited.contains(name), "$name is visited twice")
                visited.add(name)
            }
            assertEquals(visitedExpected, visited, "Breadth-first visited results differ")
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withTotalDirectoryFilter() {
        val basedir = createTestFiles()
        try {
            val referenceNames = emptySet<String>()
            compareWalkResults(referenceNames, basedir) { false }
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withTotalDirectoryFilterBreadthFirst() {
        val basedir = createTestFiles()
        try {
            val visited = HashSet<String>()
            for (file in basedir.walkBreadthFirst().onEnter { false }) {
                val name = file.toRelativeString(basedir)
                assertFalse(visited.contains(name), "$name is visited twice")
                visited.add(name)
            }
            // Expect the root directory to be visited
            assertEquals(setOf(""), visited, "Breadth-first walk results differ")
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withForEach() {
        val basedir = createTestFiles()
        try {
            var i = 0
            basedir.walkTopDown().forEach { _ -> i++ }
            assertEquals(10, i)
            i = 0
            basedir.walkBottomUp().forEach { _ -> i++ }
            assertEquals(10, i)
            i = 0
            basedir.walkBreadthFirst().forEach { _ -> i++ }
            assertEquals(10, i)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withCount() {
        val basedir = createTestFiles()
        try {
            assertEquals(10, basedir.walkTopDown().count());
            assertEquals(10, basedir.walkBottomUp().count());
            assertEquals(10, basedir.walkBreadthFirst().count());
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withReduce() {
        val basedir = createTestFiles()
        try {
            var res = basedir.walkTopDown().reduce { a, b -> if (a.canonicalPath > b.canonicalPath) a else b }
            assertTrue(res.endsWith("9.txt"), "Expected end with 9.txt actual: ${res.name}")

            res = basedir.walkBreadthFirst().reduce { a, b -> if (a.canonicalPath > b.canonicalPath) a else b }
            assertTrue(res.endsWith("9.txt"), "Expected end for breadth-first with 9.txt actual: ${res.name}")
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
                    onFail(::visitDirectoryFailed).forEach { if (!it.isDirectory) visitFile(it) }
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
                    forEach { if (it != basedir) visitFile(it) }
            assertTrue(stack.isEmpty())
            assertEquals(setOf(File("")), dirs)
            for (file in arrayOf("1", "6", "7.txt", "8")) {
                assertTrue(files.contains(File(file)), file)
            }

            //restrict access
            if (File(basedir, "1").setReadable(false)) {
                try {
                    files.clear()
                    dirs.clear()
                    basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).
                            onFail(::visitDirectoryFailed).forEach { if (!it.isDirectory) visitFile(it) }
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

    @Test fun withDepthBreadthFirst() {
        val basedir = createTestFiles()
        File(basedir, "8/10").mkdir()
        File(basedir, "8/10/11").mkdir()
        File(basedir, "8/10/11/12.txt").mkdir()
        try {
            val files = mutableListOf<File>()
            val dirs = mutableSetOf<File>()
            fun afterVisitDirectory(dir: File): Boolean {
                dirs.add(dir.relativeToOrSelf(basedir))
                return true
            }

            fun visitFile(file: File) {
                files.add(file.relativeToOrSelf(basedir))
            }

            fun assertDirsAndFiles(
                expectedDirNames: List<String>, expectedFileNames: List<String>,
                nonVisitedFiles: List<String>
            ) {
                assertEquals(expectedDirNames.map { File(it) }.toSet(), dirs)
                for (file in expectedFileNames.map { File(it) }) {
                    assertTrue(files.contains(file), file.name)
                }
                for (file in nonVisitedFiles.map { File(it) }) {
                    assertFalse(files.contains(file), file.name)
                }
                files.clear()
                dirs.clear()
            }

            data class TestCase(val depth: Int, val expectedDirNames: List<String>,
                                val expectedFileNames: List<String>,
                                val nonVisitedFiles: List<String> = emptyList()
            )

            for ((depth, expectedDirNames, expectedFileNames, nonVisitedFiles) in listOf(
                TestCase(
                    1,
                    listOf("", "1", "6", "8"),
                    listOf("1", "6", "7.txt", "8", "1/3", "1/2", "8/9.txt", "8/10"),
                    listOf("8/10/11", "8/10/11/12.txt")
                ),
                TestCase(
                    2,
                    listOf("", "1", "6", "8", "1/3", "1/2", "8/10"),
                    listOf("1", "6", "7.txt", "8", "1/3", "1/2", "8/9.txt", "8/10", "8/10/11"),
                    listOf("8/10/11/12.txt")
                ),
                TestCase(
                    3,
                    listOf("", "1", "6", "8", "1/3", "1/2", "8/10", "8/10/11"),
                    listOf("1", "6", "7.txt", "8", "1/3", "1/2", "8/9.txt", "8/10", "8/10/11", "8/10/11/12.txt"),
                )
            )) {
                basedir.walkBreadthFirst().onEnter(::afterVisitDirectory).maxDepth(depth)
                    .forEach { if (it != basedir) visitFile(it) }
                assertDirsAndFiles(
                    expectedDirNames,
                    expectedFileNames,
                    nonVisitedFiles
                )
            }
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun withRestrictedAccessBreadthFirst() {
        val basedir = createTestFiles()
        File(basedir, "1/10").mkdir()
        File(basedir, "1/10/11.txt").mkdir()
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
                failed.add(dir.name)
            }

            //restrict access
            if (File(basedir, "1").setReadable(false)) {
                try {
                    basedir.walkBreadthFirst().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).onFail(::visitDirectoryFailed)
                        .forEach { if (!it.isDirectory) visitFile(it) }
                    assertTrue(stack.isEmpty())
                    assertEquals(setOf("1"), failed)
                    assertEquals(listOf("", "1", "6", "8").map { File(it) }.toSet(), dirs)
                    assertEquals(listOf("7.txt", "8/9.txt").map { File(it) }.toSet(), files)
                    assertFalse(dirs.contains(File("1/10")))
                    assertFalse(files.contains(File("1/10/11.txt")))
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
            val bakFile = File("$file.bak")
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
            basedir.walkTopDown().takeWhile { count == 0 }.forEach {
                if (it.name == "4.txt") {
                    count++
                }
            }
            assertEquals(1, count)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test fun findBreadthFirst() {
        val basedir = createTestFiles()
        try {
            File(basedir, "8/4.txt").createNewFile()
            var count = 0
            basedir.walkBreadthFirst().takeWhile { count == 0 }.forEach {
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

    @Test fun findGitsBreadthFirst() {
        val basedir = createTestFiles()
        try {
            File(basedir, "1/3/.git").mkdir()
            File(basedir, "1/2/.git").mkdir()
            File(basedir, "6/.git").mkdir()
            val found = HashSet<File>()
            for (file in basedir.walkBreadthFirst()) {
                if (file.name == ".git") {
                    found.add(file.parentFile)
                }
            }
            assertEquals(3, found.size)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    @Test fun streamFileTreeBreadthFirst() {
        val dir = createTempDir()
        try {
            val subDir1 = createTempDir(prefix = "d1_", directory = dir)
            val subDir2 = createTempDir(prefix = "d2_", directory = dir)
            createTempDir(prefix = "d1_", directory = subDir1)
            createTempFile(prefix = "f1_", directory = subDir1)
            createTempDir(prefix = "d1_", directory = subDir2)
            assertEquals(6, dir.walkBreadthFirst().count())
        } finally {
            dir.deleteRecursively()
        }
        dir.mkdir()
        try {
            val it = dir.walkBreadthFirst().iterator()
            it.next()
            assertFailsWith<NoSuchElementException>("Second call to next() should fail.") { it.next() }
        } finally {
            dir.delete()
        }
    }

    @Test fun breadthFirstCallbacksOrder() {
        val actualCallbackOrder = mutableListOf<String>()
        val expectedCallbackOrder = listOf(
            "visit tmp\\d+.tmp",
            "onEnter tmp\\d+.tmp",
            "visit 1",
            "visit 4.txt",
            "visit 2.txt",
            "visit 3",
            "onExit tmp\\d+.tmp",
            "onEnter 1",
            "onExit 1",
            "onEnter 3",
            "visit 5.txt",
            "visit 6.txt",
            "onExit 3",
        ).map { Regex(it) }

        fun onEnter(directory: File): Boolean {
            actualCallbackOrder.add("onEnter ${directory.name}")
            return true
        }

        fun onLeave(file: File) {
            actualCallbackOrder.add("onExit ${file.name}")
        }

        val basedir = createTestFiles(listOf("1", "2.txt", "3", "3/5.txt", "3/6.txt", "4.txt"))
        try {
            basedir.walkBreadthFirst().onEnter(::onEnter).onLeave(::onLeave).forEach {
                actualCallbackOrder.add("visit ${it.name}")
            }
            assertTrue { actualCallbackOrder.size == expectedCallbackOrder.size }
            val actualAndExpected = actualCallbackOrder.zip(expectedCallbackOrder)
            for (pair in actualAndExpected) {
                assertTrue { pair.second.matches(pair.first) }
            }
        } finally {
            basedir.deleteRecursively()
        }
    }
}
