/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.nio.file.FileSystemLoopException
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class PathTreeWalkTest : AbstractPathTest() {

    companion object {
        val referenceFilenames = listOf("1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt", "6", "7.txt", "8", "8/9.txt")
        val referenceFilesOnly = listOf("1/3/4.txt", "1/3/5.txt", "7.txt", "8/9.txt")

        fun createTestFiles(): Path {
            val basedir = createTempDirectory()
            for (name in referenceFilenames) {
                val file = basedir.resolve(name)
                if (file.extension.isEmpty())
                    file.createDirectories()
                else
                    file.createFile()
            }
            return basedir
        }

        fun testVisitedFiles(expected: List<String>, walk: Sequence<Path>, basedir: Path, message: (() -> String)? = null) {
            val actual = walk.map { it.relativeToOrSelf(basedir).invariantSeparatorsPathString }
            assertEquals(expected.sorted(), actual.toList().sorted(), message?.invoke())
        }
    }

    @Test
    fun visitOnce() {
        val basedir = createTestFiles().cleanupRecursively()
        testVisitedFiles(referenceFilesOnly, basedir.walk(), basedir)

        val expected = listOf("") + referenceFilenames
        testVisitedFiles(expected, basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES), basedir)
    }

    @Test
    fun singleFile() {
        val testFile = createTempFile().cleanup()
        val nonExistentFile = testFile.resolve("foo")

        assertEquals(testFile, testFile.walk().single())
        assertEquals(testFile, testFile.walk(PathWalkOption.INCLUDE_DIRECTORIES).single())

        assertTrue(nonExistentFile.walk().none())
        assertTrue(nonExistentFile.walk(PathWalkOption.INCLUDE_DIRECTORIES).none())
    }

    @Test
    fun singleEmptyDirectory() {
        val testDir = createTempDirectory().cleanup()
        assertTrue(testDir.walk().none())
        assertEquals(testDir, testDir.walk(PathWalkOption.INCLUDE_DIRECTORIES).single())
    }

    @Test
    fun filterAndMap() {
        val basedir = createTestFiles().cleanupRecursively()
        testVisitedFiles(referenceFilesOnly, basedir.walk().filterNot { it.isDirectory() }, basedir)
    }

    @Test
    fun deleteTxtChildrenOnVisit() {

        fun visit(path: Path) {
            if (!path.isDirectory()) return

            for (child in path.listDirectoryEntries()) {
                if (child.name.endsWith("txt"))
                    child.deleteExisting()
            }
        }

        val basedir = createTestFiles().cleanupRecursively()
        val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES).onEach { visit(it) }
        val expected = listOf("", "1", "1/2", "1/3", "6", "8")
        testVisitedFiles(expected, walk, basedir)
    }

    @Test
    fun deleteSubtreeOnVisit() {
        val basedir = createTestFiles().cleanupRecursively()
        val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES).onEach { path ->
            if (path.name == "1") {
                path.toFile().deleteRecursively()
            }
        }

        val expected = listOf("", "1", "6", "7.txt", "8", "8/9.txt")
        testVisitedFiles(expected, walk, basedir)
    }

    @Test
    fun addChildOnVisit() {
        val basedir = createTestFiles().cleanupRecursively()
        val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES).onEach { path ->
            if (path.isDirectory()) {
                path.resolve("a.txt").createFile()
            }
        }

        val expected = referenceFilenames + listOf("", "a.txt", "1/a.txt", "1/2/a.txt", "1/3/a.txt", "6/a.txt", "8/a.txt")
        testVisitedFiles(expected, walk, basedir)
    }

    @Test
    fun exceptionOnVisit() {
        val basedir = createTestFiles().cleanupRecursively()
        val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES).onEach { path ->
            if (path.name == "3") {
                throw RuntimeException("Test error")
            }
        }

        val error = assertFailsWith<RuntimeException> {
            walk.toList()
        }
        assertEquals("Test error", error.message)
    }

    @Test
    fun restrictedRead() {
        val basedir = createTestFiles().cleanupRecursively()
        val restrictedDir = basedir.resolve("1/3")

        withRestrictedRead(restrictedDir) {
            val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES)

            val error = assertFailsWith<java.nio.file.AccessDeniedException> {
                walk.toList()
            }
            assertEquals(restrictedDir.toString(), error.file)
        }
    }

    @Test
    fun depthFirstOrder() {
        val basedir = createTestFiles().cleanupRecursively()

        val visited = HashSet<Path>()

        fun visit(path: Path) {
            if (path == basedir) {
                assertTrue(visited.isEmpty())
            } else {
                assertTrue(visited.contains(path.parent))
            }
            visited.add(path)
        }

        val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES).onEach(::visit)

        val expected = referenceFilenames + listOf("")
        testVisitedFiles(expected, walk, basedir)
        assertEquals(expected.sorted(), visited.map { it.relativeToOrSelf(basedir).invariantSeparatorsPathString }.sorted())
    }

    @Test
    fun addSiblingOnVisit() {
        fun makeBackup(file: Path) {
            val bakFile = Path("$file.bak")
            file.copyTo(bakFile)
        }

        val basedir = createTestFiles().cleanupRecursively()

        // added siblings do not appear during iteration
        testVisitedFiles(referenceFilesOnly, basedir.walk().onEach(::makeBackup), basedir)

        val expected = referenceFilenames + referenceFilesOnly.map { "$it.bak" } + listOf("")
        testVisitedFiles(expected, basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES), basedir)
    }

    @Test
    fun find() {
        val basedir = createTestFiles().cleanupRecursively()
        basedir.resolve("8/4.txt").createFile()
        val count = basedir.walk().count { it.name == "4.txt" }
        assertEquals(2, count)
    }

    @Test
    fun symlinkToFile() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("8/9.txt")
        basedir.resolve("1/3/link").tryCreateSymbolicLinkTo(original) ?: return

        for (followLinks in listOf(emptyArray(), arrayOf(PathWalkOption.FOLLOW_LINKS))) {
            val walk = basedir.walk(*followLinks)
            testVisitedFiles(referenceFilesOnly + listOf("1/3/link"), walk, basedir)
        }

        original.deleteExisting()
        for (followLinks in listOf(emptyArray(), arrayOf(PathWalkOption.FOLLOW_LINKS))) {
            val walk = basedir.walk(*followLinks)
            testVisitedFiles(referenceFilesOnly - listOf("8/9.txt") + listOf("1/3/link"), walk, basedir)
        }
    }

    @Test
    fun symlinkToDirectory() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("8")
        basedir.resolve("1/3/link").tryCreateSymbolicLinkTo(original) ?: return

        // directory "8" contains "9.txt" file
        val followWalk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES, PathWalkOption.FOLLOW_LINKS)
        testVisitedFiles(referenceFilenames + listOf("", "1/3/link", "1/3/link/9.txt"), followWalk, basedir)

        val nofollowWalk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES)
        testVisitedFiles(referenceFilenames + listOf("", "1/3/link"), nofollowWalk, basedir)

        original.toFile().deleteRecursively()
        for (followLinks in listOf(emptyArray(), arrayOf(PathWalkOption.FOLLOW_LINKS))) {
            val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES, *followLinks)
            testVisitedFiles(referenceFilenames - listOf("8", "8/9.txt") + listOf("", "1/3/link"), walk, basedir)
        }
    }

    @Test
    fun symlinkTwoPointingToEachOther() {
        val basedir = createTempDirectory().cleanupRecursively()
        val link1 = basedir.resolve("link1")
        val link2 = basedir.resolve("link2").tryCreateSymbolicLinkTo(link1) ?: return
        link1.tryCreateSymbolicLinkTo(link2) ?: return

        val walk = basedir.walk(PathWalkOption.FOLLOW_LINKS)

        testVisitedFiles(listOf("link1", "link2"), walk, basedir)
    }

    @Test
    fun symlinkPointingToItself() {
        val basedir = createTempDirectory().cleanupRecursively()
        val link = basedir.resolve("link")
        link.tryCreateSymbolicLinkTo(link) ?: return

        val walk = basedir.walk(PathWalkOption.FOLLOW_LINKS)

        testVisitedFiles(listOf("link"), walk, basedir)
    }

    @Test
    fun symlinkToSymlink() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("8")
        val link = basedir.resolve("1/3/link").tryCreateSymbolicLinkTo(original) ?: return
        basedir.resolve("1/linkToLink").tryCreateSymbolicLinkTo(link) ?: return

        val walk = basedir.walk(PathWalkOption.INCLUDE_DIRECTORIES, PathWalkOption.FOLLOW_LINKS)

        val depth2ExpectedNames =
                listOf("", "1", "1/2", "1/3", "1/linkToLink", "6", "7.txt", "8", "8/9.txt") // linkToLink is visited
        val depth3ExpectedNames = depth2ExpectedNames +
                listOf("1/3/4.txt", "1/3/5.txt", "1/3/link", "1/linkToLink/9.txt") // "9.txt" is visited once more through linkToLink
        val depth4ExpectedNames = depth3ExpectedNames +
                listOf("1/3/link/9.txt") // "9.txt" is visited once more through link
        testVisitedFiles(depth4ExpectedNames, walk, basedir) // no depth limit
    }

    @Test
    fun symlinkBasedir() {
        val basedir = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(basedir) ?: return

        run {
            val followWalk = link.walk(PathWalkOption.INCLUDE_DIRECTORIES, PathWalkOption.FOLLOW_LINKS)
            testVisitedFiles(referenceFilenames + listOf(""), followWalk, link)
            testVisitedFiles(referenceFilesOnly, link.walk(PathWalkOption.FOLLOW_LINKS), link)

            val nofollowWalk = link.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            assertEquals(link, nofollowWalk.single())
            assertEquals(link, link.walk().single())
        }

        run {
            basedir.toFile().deleteRecursively()

            val followWalk = link.walk(PathWalkOption.INCLUDE_DIRECTORIES, PathWalkOption.FOLLOW_LINKS)
            assertEquals(link, followWalk.single())

            val nofollowWalk = link.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            assertEquals(link, nofollowWalk.single())
        }
    }

    @Test
    fun symlinkCyclic() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("1")
        val link = original.resolve("2/link").tryCreateSymbolicLinkTo(original) ?: return

        for (order in listOf(arrayOf(), arrayOf(PathWalkOption.BREADTH_FIRST))) {
            val walk = basedir.walk(PathWalkOption.FOLLOW_LINKS, *order)
            val error = assertFailsWith<FileSystemLoopException> {
                walk.toList()
            }
            assertEquals(link.toString(), error.file)
        }
    }

    @Test
    fun symlinkCyclicWithTwo() {
        val basedir = createTestFiles().cleanupRecursively()
        val dir8 = basedir.resolve("8")
        val dir2 = basedir.resolve("1/2")
        dir8.resolve("linkTo2").tryCreateSymbolicLinkTo(dir2) ?: return
        dir2.resolve("linkTo8").tryCreateSymbolicLinkTo(dir8) ?: return

        for (order in listOf(arrayOf(), arrayOf(PathWalkOption.BREADTH_FIRST))) {
            val walk = basedir.walk(PathWalkOption.FOLLOW_LINKS, *order)
            assertFailsWith<FileSystemLoopException> {
                walk.toList()
            }
        }
    }

    @Test
    fun breadthFirstOrder() {
        val basedir = createTestFiles().cleanupRecursively()
        val walk = basedir.walk(PathWalkOption.BREADTH_FIRST, PathWalkOption.INCLUDE_DIRECTORIES)
        val depth0 = mutableListOf("")
        val depth1 = mutableListOf("1", "6", "7.txt", "8")
        val depth2 = mutableListOf("1/2", "1/3", "8/9.txt")
        val depth3 = mutableListOf("1/3/4.txt", "1/3/5.txt")

        for (file in walk) {
            when (val pathString = file.relativeToOrSelf(basedir).invariantSeparatorsPathString) {
                in depth0 -> {
                    depth0.remove(pathString)
                }
                in depth1 -> {
                    assertTrue(depth0.isEmpty())
                    depth1.remove(pathString)
                }
                in depth2 -> {
                    assertTrue(depth1.isEmpty())
                    depth2.remove(pathString)
                }
                in depth3 -> {
                    assertTrue(depth2.isEmpty())
                    depth3.remove(pathString)
                }
                else -> {
                    fail("Unexpected file: $file. It might have appeared for the second time.")
                }
            }
        }

        assertTrue(
            depth0.isEmpty() && depth1.isEmpty() && depth2.isEmpty() && depth3.isEmpty(),
            "The following files were not visited: $depth0, $depth1, $depth2 $depth3"
        )
    }

    @Test
    fun breadthFirstOnlyFiles() {
        val basedir = createTestFiles().cleanupRecursively()
        val walk = basedir.walk(PathWalkOption.BREADTH_FIRST)

        val depth1 = mutableListOf("7.txt")
        val depth2 = mutableListOf("8/9.txt")
        val depth3 = mutableListOf("1/3/4.txt", "1/3/5.txt")

        for (file in walk) {
            when (val pathString = file.relativeToOrSelf(basedir).invariantSeparatorsPathString) {
                in depth1 -> {
                    depth1.remove(pathString)
                }
                in depth2 -> {
                    assertTrue(depth1.isEmpty())
                    depth2.remove(pathString)
                }
                in depth3 -> {
                    assertTrue(depth2.isEmpty())
                    depth3.remove(pathString)
                }
                else -> {
                    fail("Unexpected file: $file. It might have appeared for the second time.")
                }
            }
        }

        assertTrue(
            depth1.isEmpty() && depth2.isEmpty() && depth3.isEmpty(),
            "The following files were not visited: $depth1, $depth2 $depth3"
        )
    }
}