/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.IOException
import java.nio.file.*
import kotlin.io.path.*
import kotlin.test.*

class PathExtensionsTest {
    private val isCaseInsensitiveFileSystem = Paths.get("C:/") == Paths.get("c:/")
    private val isBackslashSeparator = FileSystems.getDefault().separator == "\\"

    @Test
    fun extension() {
        assertEquals("bbb", Paths.get("aaa.bbb").extension)
        assertEquals("", Paths.get("aaa").extension)
        assertEquals("", Paths.get("aaa.").extension)
        assertEquals("bbb", Paths.get(".bbb").extension)
        assertEquals("", Paths.get("/my.dir/log").extension)
        assertEquals("", Paths.get("/").extension)
    }

    @Test
    fun nameWithoutExtension() {
        assertEquals("aaa", Paths.get("aaa.bbb").nameWithoutExtension)
        assertEquals("aaa", Paths.get("aaa").nameWithoutExtension)
        assertEquals("aaa", Paths.get("aaa.").nameWithoutExtension)
        assertEquals("", Paths.get(".bbb").nameWithoutExtension)
        assertEquals("log", Paths.get("/my.dir/log").nameWithoutExtension)
        assertEquals("", Paths.get("").nameWithoutExtension)
        assertEquals("", Paths.get("/").nameWithoutExtension)
    }

    @Test
    fun testCopyTo() {
        val srcFile = Files.createTempFile(null, null)
        val dstFile = Files.createTempFile(null, null)
        try {
            srcFile.writeText("Hello, World!")
            assertFailsWith<FileAlreadyExistsException>("copy do not overwrite existing file") {
                srcFile.copyTo(dstFile)
            }

            var dst = srcFile.copyTo(dstFile, overwrite = true)
            assertSame(dst, dstFile)
            compareFiles(srcFile, dst, "copy with overwrite over existing file")

            srcFile.copyTo(srcFile)
            srcFile.copyTo(srcFile, overwrite = true)
            compareFiles(dst, srcFile, "copying file to itself leaves it intact")

            assertTrue(Files.deleteIfExists(dstFile))
            dst = srcFile.copyTo(dstFile)
            compareFiles(srcFile, dst, "copy to new file")

            val subDst = dstFile.resolve("foo/bar")
            assertFailsWith<NoSuchFileException> { srcFile.copyTo(subDst) }
            assertFailsWith<NoSuchFileException> { srcFile.copyTo(subDst, overwrite = true) }
            assertTrue(Files.deleteIfExists(dstFile))
            assertFailsWith<NoSuchFileException> { srcFile.copyTo(subDst) }

            Files.createDirectory(dstFile)
            val child = dstFile.resolve("child")
            Files.createFile(child)
            assertFailsWith<DirectoryNotEmptyException>( "copy with overwrite do not overwrite non-empty dir") {
                srcFile.copyTo(dstFile, overwrite = true)
            }
            Files.delete(child)

            srcFile.copyTo(dstFile, overwrite = true)
            assertEquals(srcFile.readText(), dstFile.readText(), "copy with overwrite over empty dir")

            assertTrue(Files.deleteIfExists(srcFile))
            assertTrue(Files.deleteIfExists(dstFile))

            assertFailsWith<NoSuchFileException> {
                srcFile.copyTo(dstFile)
            }

            Files.createDirectory(srcFile)
            srcFile.resolve("somefile").writeText("some content")
            dstFile.writeText("")
            assertFailsWith<FileAlreadyExistsException>("copy dir do not overwrite file") {
                srcFile.copyTo(dstFile)
            }
            srcFile.copyTo(dstFile, overwrite = true)
            assertTrue(dstFile.isDirectory())
            assertTrue(dstFile.listDirectoryEntries().isEmpty(), "only directory is copied, but not its content")

            assertFailsWith<FileAlreadyExistsException>("copy dir do not overwrite dir") {
                srcFile.copyTo(dstFile)
            }

            srcFile.copyTo(dstFile, overwrite = true)
            assertTrue(dstFile.isDirectory())
            assertTrue(dstFile.listDirectoryEntries().isEmpty(), "only directory is copied, but not its content")

            dstFile.resolve("somefile2").writeText("some content2")
            assertFailsWith<DirectoryNotEmptyException>("copy dir do not overwrite non-empty dir") {
                srcFile.copyTo(dstFile, overwrite = true)
            }
        } finally {
            srcFile.toFile().deleteRecursively()
            dstFile.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyToNameWithoutParent() {
        val currentDir = Paths.get("").toAbsolutePath()
        val srcFile = Files.createTempFile(null, null)
        val dstFile = Files.createTempFile(currentDir, null, null)
        try {
            srcFile.writeText("Hello, World!", Charsets.UTF_8)
            Files.delete(dstFile)

            val dstRelative = Paths.get(dstFile.fileName.toString())

            srcFile.copyTo(dstRelative)

            assertEquals(srcFile.readText(), dstFile.readText())
        } finally {
            Files.delete(dstFile)
            Files.delete(srcFile)
        }
    }

    private fun compareFiles(src: Path, dst: Path, message: String? = null) {
        assertTrue(dst.exists())
        assertEquals(src.isRegularFile(), dst.isRegularFile(), message)
        if (dst.isRegularFile()) {
            assertTrue(src.readBytes().contentEquals(dst.readBytes()), message)
        }
    }

    @Test
    fun testAttributeGettersOnFile() {
        val file = Files.createTempFile(null, null)
        assertTrue(file.exists())
        assertFalse(file.notExists())
        assertTrue(file.isRegularFile())
        assertFalse(file.isDirectory())
        assertFalse(file.isSymbolicLink())
        assertTrue(file.isReadable())
        assertTrue(file.isWritable())
        assertTrue(file.isSameFile(file))

        // The default value of these depends on the current operating system, so just check that
        // they don't throw an exception.
        file.isExecutable()
        file.isHidden()
    }

    @Test
    fun testAttributeGettersOnDirectory() {
        val file = Files.createTempDirectory(null)
        assertTrue(file.exists())
        assertFalse(file.notExists())
        assertFalse(file.isRegularFile())
        assertTrue(file.isDirectory())
        assertFalse(file.isSymbolicLink())
        assertTrue(file.isReadable())
        assertTrue(file.isWritable())
        assertTrue(file.isSameFile(file))

        file.isExecutable()
        file.isHidden()
    }

    @Test
    fun testAttributeGettersOnNonExistentPath() {
        val file = Files.createTempDirectory(null).resolve("foo")
        assertFalse(file.exists())
        assertTrue(file.notExists())
        assertFalse(file.isRegularFile())
        assertFalse(file.isDirectory())
        assertFalse(file.isSymbolicLink())
        assertFalse(file.isReadable())
        assertFalse(file.isWritable())
        assertTrue(file.isSameFile(file))

        file.isExecutable()
        // This function will either throw an exception or return false,
        // depending on the operating system.
        try {
            assertFalse(file.isHidden())
        } catch (e: IOException) {
        }
    }

    @Test
    fun testListDirectoryEntries() {
        val dir = Files.createTempDirectory(null)
        assertEquals(0, dir.listDirectoryEntries().size)

        val file = dir.resolve("f1")
        Files.createFile(file)
        assertEquals(listOf(file), dir.listDirectoryEntries())

        assertFailsWith<NotDirectoryException> { file.listDirectoryEntries() }
    }

    @Test
    fun testUseDirectoryEntries() {
        val dir = Files.createTempDirectory(null)
        assertEquals(0, dir.useDirectoryEntries { it.toList() }.size)

        val file = dir.resolve("f1")
        Files.createFile(file)
        assertEquals(listOf(file), dir.useDirectoryEntries { it.toList() })

        assertFailsWith<NotDirectoryException> { file.useDirectoryEntries { it.toList() } }
    }

    @Test
    fun testForEachDirectoryEntry() {
        val dir = Files.createTempDirectory(null)
        val entries = mutableListOf<Path>()

        dir.forEachDirectoryEntry { entries.add(it) }
        assertTrue(entries.isEmpty())

        val file = dir.resolve("f1")
        Files.createFile(file)
        dir.forEachDirectoryEntry { entries.add(it) }
        assertEquals(listOf(file), entries)

        assertFailsWith<NotDirectoryException> { file.forEachDirectoryEntry { } }
    }


    private fun testRelativeTo(expected: String?, path: String, base: String) =
        testRelativeTo(expected?.let { Paths.get(it) }, Paths.get(path), Paths.get(base))
    private fun testRelativeTo(expected: String, path: Path, base: Path) =
        testRelativeTo(Paths.get(expected), path, base)

    private fun testRelativeTo(expected: Path?, path: Path, base: Path) {
        val context = "path: '$path', base: '$base'"
        if (expected != null) {
            assertEquals(expected, path.relativeTo(base), context)
        } else {
            val e = assertFailsWith<IllegalArgumentException>(context) { path.relativeTo(base) }
            val message = assertNotNull(e.message)
            assertTrue(path.toString() in message, message)
            assertTrue(base.toString() in message, message)
        }
        assertEquals(expected, path.relativeToOrNull(base), context)
        assertEquals(expected ?: path, path.relativeToOrSelf(base), context)
    }

    @Test
    fun relativeToRooted() {
        val file1 = "/foo/bar/baz"
        val file2 = "/foo/baa/ghoo"

        testRelativeTo("../../bar/baz", file1, file2)

        val file3 = "/foo/bar"

        testRelativeTo("baz", file1, file3)
        testRelativeTo("..", file3, file1)

        val file4 = "/foo/bar/"

        testRelativeTo("baz", file1, file4)
        testRelativeTo("..", file4, file1)
        testRelativeTo("", file3, file4)
        testRelativeTo("", file4, file3)

        val file5 = "/foo/baran"

        testRelativeTo("../bar", file3, file5)
        testRelativeTo("../baran", file5, file3)
        testRelativeTo("../bar", file4, file5)
        testRelativeTo("../baran", file5, file4)

        if (isBackslashSeparator) {
            val file6 = "C:\\Users\\Me"
            val file7 = "C:\\Users\\Me\\Documents"

            testRelativeTo("..", file6, file7)
            testRelativeTo("Documents", file7, file6)

            val file8 = """\\my.host\home/user/documents/vip"""
            val file9 = """\\my.host\home/other/images/nice"""

            testRelativeTo("../../../user/documents/vip", file8, file9)
            testRelativeTo("../../../other/images/nice", file9, file8)
        }

        if (isCaseInsensitiveFileSystem) {
            testRelativeTo("bar", "C:/bar", "c:/")
        }
    }

    @Test
    fun relativeToRelative() {
        val nested = Paths.get("foo/bar")
        val base = Paths.get("foo")

        testRelativeTo("bar", nested, base)
        testRelativeTo("..", base, nested)

        val empty = Paths.get("")
        val current = Paths.get(".")
        val parent = Paths.get("..")
        val outOfRoot = Paths.get("../bar")

        testRelativeTo("../bar", outOfRoot, empty)
        testRelativeTo("../../bar", outOfRoot, base)
        testRelativeTo("bar", outOfRoot, parent)
        testRelativeTo("..", parent, outOfRoot)

        val root = Paths.get("/root")
        val files = listOf(nested, base, empty, outOfRoot, current, parent)
        val bases = listOf(nested, base, empty, current)

        for (file in files)
            // file should have empty path relative to itself
            testRelativeTo("", file, file)

        for (file in files) {
            @Suppress("NAME_SHADOWING")
            for (base in bases) {
                val rootedFile = root.resolve(file)
                val rootedBase = root.resolve(base)
                assertEquals(rootedFile.relativeTo(rootedBase), file.relativeTo(base), "nested: $file, base: $base")
            }
        }
    }

    @Test
    fun relativeToFails() {
        val absolute = Paths.get("/foo/bar/baz")
        val relative = Paths.get("foo/bar")
        val networkShare1 = Paths.get("""\\my.host\share1/folder""")
        val networkShare2 = Paths.get("""\\my.host\share2\folder""")

        val allFiles = listOf(absolute, relative) + if (isBackslashSeparator) listOf(networkShare1, networkShare2) else emptyList()
        for (file in allFiles) {
            for (base in allFiles) {
                if (file != base) testRelativeTo(null, file, base)
            }
        }

        if (isBackslashSeparator) {
            testRelativeTo(null, "C:/dir1", "D:/dir2")
        }

        testRelativeTo(null, "foo", "..")
        testRelativeTo(null, "../foo", "../..")
    }

    @Test
    fun relativeTo() {
        testRelativeTo("kotlin", "src/kotlin", "src")
        testRelativeTo("", "dir", "dir")
        testRelativeTo("..", "dir", "dir/subdir")
        testRelativeTo("../../test", "test", "dir/dir")
        testRelativeTo("foo/bar", "../../foo/bar", "../../sub/../.")
        testRelativeTo(null, "../../foo/bar", "../../sub/../..")
    }
}
