/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.test.*

class PathTreeWalkTest {

    companion object {
        val referenceFilenames =
            listOf("1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt", "6", "7.txt", "8", "8/9.txt")

        fun createTestFiles(): Path {
            val basedir = Files.createTempDirectory(null)
            for (name in referenceFilenames) {
                val file = basedir.resolve(name)
                if (file.extension.isEmpty())
                    Files.createDirectory(file)
                else
                    Files.createFile(file)
            }
            return basedir
        }
    }

    @Test
    fun withSimple() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("") + referenceFilenames
            val namesTopDown = HashSet<String>()
            for (file in basedir.walkTopDown()) {
                val name = basedir.relativize(file).invariantSeparatorsPath
                assertFalse(namesTopDown.contains(name), "$name is visited twice")
                namesTopDown.add(name)
            }
            assertEquals(referenceNames, namesTopDown)
            val namesBottomUp = HashSet<String>()
            for (file in basedir.walkBottomUp()) {
                val name = basedir.relativize(file).invariantSeparatorsPath
                assertFalse(namesBottomUp.contains(name), "$name is visited twice")
                namesBottomUp.add(name)
            }
            assertEquals(referenceNames, namesBottomUp)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun singleFile() {
        val testFile = Files.createTempFile(null, null)
        val nonExistantFile = testFile.resolve("foo")
        try {
            for (walk in listOf(Path::walkTopDown, Path::walkBottomUp)) {
                assertEquals(testFile, walk(testFile).single(), walk.name)
                assertEquals(
                    testFile,
                    testFile.walk().onEnter { false }.single(),
                    "${walk.name} - enter should not be called for single file"
                )

                assertTrue(walk(nonExistantFile).none(), "${walk.name} - enter should not be called for single file")
            }
        } finally {
            Files.delete(testFile)
        }
    }

    @Test
    fun withEnterLeave() {
        val basedir = createTestFiles()
        try {
            val referenceNames =
                setOf("", "1", "1/2", "6", "8")
            val namesTopDownEnter = HashSet<String>()
            val namesTopDownLeave = HashSet<String>()
            val namesTopDown = HashSet<String>()
            fun enter(file: Path): Boolean {
                val name = basedir.relativize(file).invariantSeparatorsPath
                assertTrue(file.isDirectory(), "$name is not directory, only directories should be entered")
                assertFalse(namesTopDownEnter.contains(name), "$name is entered twice")
                assertFalse(namesTopDownLeave.contains(name), "$name is left before entrance")
                if (file.fileName.toString() == "3") return false // filter out 3
                namesTopDownEnter.add(name)
                return true
            }

            fun leave(file: Path) {
                val name = basedir.relativize(file).invariantSeparatorsPath
                assertTrue(file.isDirectory(), "$name is not directory, only directories should be left")
                assertFalse(namesTopDownLeave.contains(name), "$name is left twice")
                namesTopDownLeave.add(name)
                assertTrue(namesTopDownEnter.contains(name), "$name is left before entrance")
            }

            fun visit(file: Path) {
                val name = basedir.relativize(file).invariantSeparatorsPath
                if (file.isDirectory()) {
                    assertTrue(namesTopDownEnter.contains(name), "$name is visited before entrance")
                    namesTopDown.add(name)
                    assertFalse(namesTopDownLeave.contains(name), "$name is visited after leaving")
                }
                if (file == basedir)
                    return
                val parent = file.parent
                if (parent != null) {
                    val parentName = basedir.relativize(parent).invariantSeparatorsPath
                    assertTrue(
                        namesTopDownEnter.contains(parentName),
                        "$name is visited before entering its parent $parentName"
                    )
                    assertFalse(
                        namesTopDownLeave.contains(parentName),
                        "$name is visited after leaving its parent $parentName"
                    )
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

    @Test
    fun withFilterAndMap() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            assertEquals(referenceNames, basedir.walkTopDown().filter { it.isDirectory() }.map {
                basedir.relativize(it).invariantSeparatorsPath
            }.toHashSet())
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun withDeleteTxtTopDown() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            val namesTopDown = HashSet<String>()
            fun enter(file: Path) {
                assertTrue(file.isDirectory())
                for (child in file.listFiles()) {
                    if (child.fileName.toString().endsWith("txt"))
                        Files.delete(child)
                }
            }
            for (file in basedir.walkTopDown().onEnter { enter(it); true }) {
                val name = basedir.relativize(file).invariantSeparatorsPath
                assertFalse(namesTopDown.contains(name), "$name is visited twice")
                namesTopDown.add(name)
            }
            assertEquals(referenceNames, namesTopDown)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun withDeleteTxtBottomUp() {
        val basedir = createTestFiles()
        try {
            val referenceNames = setOf("", "1", "1/2", "1/3", "6", "8")
            val namesTopDown = HashSet<String>()
            fun enter(file: Path) {
                assertTrue(file.isDirectory())
                for (child in file.listFiles()) {
                    if (child.fileName.toString().endsWith("txt"))
                        Files.delete(child)
                }
            }
            for (file in basedir.walkBottomUp().onEnter { enter(it); true }) {
                val name = basedir.relativize(file).invariantSeparatorsPath
                assertFalse(namesTopDown.contains(name), "$name is visited twice")
                namesTopDown.add(name)
            }
            assertEquals(referenceNames, namesTopDown)
        } finally {
            basedir.deleteRecursively()
        }
    }

    private fun compareWalkResults(expected: Set<String>, basedir: Path, filter: (Path) -> Boolean) {
        val namesTopDown = HashSet<String>()
        for (file in basedir.walkTopDown().onEnter { filter(it) }) {
            val name = basedir.relativize(file).toString()
            assertFalse(namesTopDown.contains(name), "$name is visited twice")
            namesTopDown.add(name)
        }
        assertEquals(expected, namesTopDown, "Top-down walk results differ")
        val namesBottomUp = HashSet<String>()
        for (file in basedir.walkBottomUp().onEnter { filter(it) }) {
            val name = basedir.relativize(file).toString()
            assertFalse(namesBottomUp.contains(name), "$name is visited twice")
            namesBottomUp.add(name)
        }
        assertEquals(expected, namesBottomUp, "Bottom-up walk results differ")
    }

    @Test
    fun withDirectoryFilter() {
        val basedir = createTestFiles()
        try {
            // Every directory ended with 3 and its content is filtered out
            fun filter(file: Path): Boolean = !file.fileName.toString().endsWith("3")

            val referenceNames = listOf("", "1", "1/2", "6", "7.txt", "8", "8/9.txt").map { Paths.get(it).toString() }.toSet()
            compareWalkResults(referenceNames, basedir, ::filter)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun withTotalDirectoryFilter() {
        val basedir = createTestFiles()
        try {
            val referenceNames = emptySet<String>()
            compareWalkResults(referenceNames, basedir) { false }
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun withForEach() {
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

    @Test
    fun withCount() {
        val basedir = createTestFiles()
        try {
            assertEquals(10, basedir.walkTopDown().count());
            assertEquals(10, basedir.walkBottomUp().count());
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun withReduce() {
        val basedir = createTestFiles()
        try {
            val res = basedir.walkTopDown().reduce { a, b -> if (a.toRealPath() > b.toRealPath()) a else b }
            assertTrue(res.endsWith("9.txt"), "Expected end with 9.txt actual: ${res.fileName}")
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun withVisitorAndDepth() {
        val basedir = createTestFiles()
        try {
            val files = HashSet<Path>()
            val dirs = HashSet<Path>()
            val failed = HashSet<String>()
            val stack = ArrayList<Path>()
            fun beforeVisitDirectory(dir: Path): Boolean {
                stack.add(dir)
                dirs.add(basedir.relativize(dir))
                return true
            }

            fun afterVisitDirectory(dir: Path) {
                assertEquals(stack.last(), dir)
                stack.removeAt(stack.lastIndex)
            }

            fun visitFile(file: Path) {
                assertTrue(stack.last().listFiles().contains(file), file.toString())
                files.add(basedir.relativize(file))
            }

            fun visitDirectoryFailed(dir: Path, @Suppress("UNUSED_PARAMETER") e: IOException) {
                assertEquals(stack.last(), dir)
                failed.add(dir.fileName.toString())
            }
            basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).onFail(::visitDirectoryFailed)
                .forEach { if (!it.isDirectory()) visitFile(it) }
            assertTrue(stack.isEmpty())
            for (fileName in arrayOf("", "1", "1/2", "1/3", "6", "8")) {
                assertTrue(dirs.contains(Paths.get(fileName)), fileName)
            }
            for (fileName in arrayOf("1/3/4.txt", "1/3/4.txt", "7.txt", "8/9.txt")) {
                assertTrue(files.contains(Paths.get(fileName)), fileName)
            }

            //limit maxDepth
            files.clear()
            dirs.clear()
            basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).maxDepth(1)
                .forEach { if (it != basedir) visitFile(it) }
            assertTrue(stack.isEmpty())
            assertEquals(setOf(Paths.get("")), dirs)
            for (file in arrayOf("1", "6", "7.txt", "8")) {
                assertTrue(files.contains(Paths.get(file)), file)
            }

            //restrict access
            val oldPermissions = basedir.resolve("1").setNotReadable()
            if (oldPermissions != null) {
                try {
                    files.clear()
                    dirs.clear()
                    basedir.walkTopDown().onEnter(::beforeVisitDirectory).onLeave(::afterVisitDirectory).onFail(::visitDirectoryFailed)
                        .forEach { if (!it.isDirectory()) visitFile(it) }
                    assertTrue(stack.isEmpty())
                    assertEquals(setOf("1"), failed)
                    assertEquals(listOf("", "1", "6", "8").map { Paths.get(it) }.toSet(), dirs)
                    assertEquals(listOf("7.txt", "8/9.txt").map { Paths.get(it) }.toSet(), files)
                } finally {
                    basedir.resolve("1").restorePermission(oldPermissions)
                }
            } else {
                System.err.println("cannot restrict access")
            }
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun topDown() {
        val basedir = createTestFiles()
        try {
            val visited = HashSet<Path>()
            val block: (Path) -> Unit = {
                assertTrue(!visited.contains(it), it.toString())
                assertTrue(it == basedir && visited.isEmpty() || visited.contains(it.parent), it.toString())
                visited.add(it)
            }
            basedir.walkTopDown().forEach(block)
            assertEquals(10, visited.size)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun restrictedAccess() {
        val basedir = createTestFiles()
        val restricted = basedir.resolve("1")
        val oldPermissions = restricted.setNotReadable()
        try {
            if (oldPermissions != null) {
                val visited = HashSet<Path>()
                val block: (Path) -> Unit = {
                    assertTrue(!visited.contains(it), it.toString())
                    assertTrue(it == basedir && visited.isEmpty() || visited.contains(it.parent), it.toString())
                    visited.add(it)
                }
                basedir.walkTopDown().forEach(block)
                assertEquals(6, visited.size)
            }
        } finally {
            restricted.restorePermission(oldPermissions)
            basedir.deleteRecursively()
        }
    }

    @Test
    fun backup() {
        var count = 0
        fun makeBackup(file: Path) {
            count++
            val bakFile = Paths.get("$file.bak")
            file.copyTo(bakFile)
        }

        val basedir1 = createTestFiles()
        try {
            basedir1.walkTopDown().forEach {
                if (it.isFile()) {
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
                if (it.isFile()) {
                    makeBackup(it)
                }
            }
            assertEquals(4, count)
        } finally {
            basedir2.deleteRecursively()
        }
    }

    @Test
    fun find() {
        val basedir = createTestFiles()
        try {
            Files.createFile(basedir.resolve("8/4.txt"))
            var count = 0
            basedir.walkTopDown().takeWhile { count == 0 }.forEach {
                if (it.fileName.toString() == "4.txt") {
                    count++
                }
            }
            assertEquals(1, count)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun findGits() {
        val basedir = createTestFiles()
        try {
            Files.createDirectory(basedir.resolve("1/3/.git"))
            Files.createDirectory(basedir.resolve("1/2/.git"))
            Files.createDirectory(basedir.resolve("6/.git"))
            val found = HashSet<Path>()
            for (file in basedir.walkTopDown()) {
                if (file.fileName.toString() == ".git") {
                    found.add(file.parent)
                }
            }
            assertEquals(3, found.size)
        } finally {
            basedir.deleteRecursively()
        }
    }

    @Test
    fun streamFileTree() {
        val dir = Files.createTempDirectory(null)
        try {
            val subDir1 = Files.createTempDirectory(dir, "d1_")
            val subDir2 = Files.createTempDirectory(dir, "d2_")
            Files.createTempDirectory(subDir1, "d1_")
            Files.createTempFile(subDir1, "f1_", null)
            Files.createTempDirectory(subDir2, "d1_")
            assertEquals(6, dir.walkTopDown().count())
        } finally {
            dir.deleteRecursively()
        }

        Files.createDirectory(dir)
        try {
            val it = dir.walkTopDown().iterator()
            it.next()
            assertFailsWith<NoSuchElementException>("Second call to next() should fail.") { it.next() }
        } finally {
            Files.delete(dir)
        }
    }
}
