/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.IOException
import java.nio.file.*
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
            assertFailsWith(FileAlreadyExistsException::class, "copy do not overwrite existing file") {
                srcFile.copyTo(dstFile)
            }

            var dst = srcFile.copyTo(dstFile, overwrite = true)
            assertSame(dst, dstFile)
            compareFiles(srcFile, dst, "copy with overwrite over existing file")

            assertTrue(Files.deleteIfExists(dstFile))
            dst = srcFile.copyTo(dstFile)
            compareFiles(srcFile, dst, "copy to new file")

            assertTrue(Files.deleteIfExists(dstFile))
            Files.createDirectory(dstFile)
            val child = dstFile.resolve("child")
            Files.createFile(child)
            assertFailsWith(DirectoryNotEmptyException::class, "copy with overwrite do not overwrite non-empty dir") {
                srcFile.copyTo(dstFile, overwrite = true)
            }
            Files.delete(child)

            srcFile.copyTo(dstFile, overwrite = true)
            assertEquals(srcFile.readText(), dstFile.readText(), "copy with overwrite over empty dir")

            assertTrue(Files.deleteIfExists(srcFile))
            assertTrue(Files.deleteIfExists(dstFile))

            assertFailsWith(NoSuchFileException::class) {
                srcFile.copyTo(dstFile)
            }

            Files.createDirectory(srcFile)
            srcFile.resolve("somefile").writeText("some content")
            dstFile.writeText("")
            assertFailsWith(FileAlreadyExistsException::class, "copy dir do not overwrite file") {
                srcFile.copyTo(dstFile)
            }
            srcFile.copyTo(dstFile, overwrite = true)
            assertTrue(dstFile.isDirectory())
            assertTrue(dstFile.listDirectoryEntries().isEmpty(), "only directory is copied, but not its content")

            assertFailsWith(FileAlreadyExistsException::class, "copy dir do not overwrite dir") {
                srcFile.copyTo(dstFile)
            }

            srcFile.copyTo(dstFile, overwrite = true)
            assertTrue(dstFile.isDirectory())
            assertTrue(dstFile.listDirectoryEntries().isEmpty(), "only directory is copied, but not its content")

            dstFile.resolve("somefile2").writeText("some content2")
            assertFailsWith(FileAlreadyExistsException::class, "copy dir do not overwrite non-empty dir") {
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

    @Test
    fun relativeToRooted() {
        val file1 = Paths.get("/foo/bar/baz")
        val file2 = Paths.get("/foo/baa/ghoo")

        assertEquals("../../bar/baz", file1.relativeTo(file2).invariantSeparatorsPath)

        val file3 = Paths.get("/foo/bar")

        assertEquals("baz", file1.relativeTo(file3).toString())
        assertEquals("..", file3.relativeTo(file1).toString())

        val file4 = Paths.get("/foo/bar/")

        assertEquals("baz", file1.relativeTo(file4).toString())
        assertEquals("..", file4.relativeTo(file1).toString())
        assertEquals("", file3.relativeTo(file4).toString())
        assertEquals("", file4.relativeTo(file3).toString())

        val file5 = Paths.get("/foo/baran")

        assertEquals("../bar", file3.relativeTo(file5).invariantSeparatorsPath)
        assertEquals("../baran", file5.relativeTo(file3).invariantSeparatorsPath)
        assertEquals("../bar", file4.relativeTo(file5).invariantSeparatorsPath)
        assertEquals("../baran", file5.relativeTo(file4).invariantSeparatorsPath)

        if (isBackslashSeparator) {
            val file6 = Paths.get("C:\\Users\\Me")
            val file7 = Paths.get("C:\\Users\\Me\\Documents")

            assertEquals("..", file6.relativeTo(file7).toString())
            assertEquals("Documents", file7.relativeTo(file6).toString())

            val file8 = Paths.get("""\\my.host\home/user/documents/vip""")
            val file9 = Paths.get("""\\my.host\home/other/images/nice""")

            assertEquals("../../../user/documents/vip", file8.relativeTo(file9).invariantSeparatorsPath)
            assertEquals("../../../other/images/nice", file9.relativeTo(file8).invariantSeparatorsPath)
        }

        if (isCaseInsensitiveFileSystem) {
            assertEquals("bar", Paths.get("C:/bar").relativeTo(Paths.get("c:/")).toString())
        }
    }

    @Test
    fun relativeToRelative() {
        val nested = Paths.get("foo/bar")
        val base = Paths.get("foo")

        assertEquals("bar", nested.relativeTo(base).toString())
        assertEquals("..", base.relativeTo(nested).toString())

        val current = Paths.get(".")
        val parent = Paths.get("..")
        val outOfRoot = Paths.get("../bar")

        assertEquals(Paths.get("../../bar"), outOfRoot.relativeTo(base))
        assertEquals("bar", outOfRoot.relativeTo(parent).toString())
        assertEquals("..", parent.relativeTo(outOfRoot).toString())

        val root = Paths.get("/root")
        val files = listOf(nested, base, outOfRoot, current, parent)
        val bases = listOf(nested, base, current)

        for (file in files)
            assertEquals("", file.relativeTo(file).toString(), "file should have empty path relative to itself: $file")

        for (file in files) {
            @Suppress("NAME_SHADOWING")
            for (base in bases) {
                val rootedFile = root.resolve(file)
                val rootedBase = root.resolve(base)
                assertEquals(file.relativeTo(base), rootedFile.relativeTo(rootedBase), "nested: $file, base: $base")
            }
        }
    }

    @Test
    fun relativeToFails() {
        val absolute = Paths.get("/foo/bar/baz")
        val relative = Paths.get("foo/bar")
        val networkShare1 = Paths.get("""\\my.host\share1/folder""")
        val networkShare2 = Paths.get("""\\my.host\share2\folder""")

        fun assertFailsRelativeTo(file: Path, base: Path) {
            val e = assertFailsWith<IllegalArgumentException>("file: $file, base: $base") { file.relativeTo(base) }
            assertNotNull(e.message)
        }

        val allFiles = listOf(absolute, relative) + if (isBackslashSeparator) listOf(networkShare1, networkShare2) else emptyList()
        for (file in allFiles) {
            for (base in allFiles) {
                if (file != base) assertFailsRelativeTo(file, base)
            }
        }

        if (isBackslashSeparator) {
            val fileOnC = Paths.get("C:/dir1")
            val fileOnD = Paths.get("D:/dir2")
            assertFailsRelativeTo(fileOnC, fileOnD)
        }
    }

    @Test
    fun relativeTo() {
        assertEquals("kotlin", Paths.get("src/kotlin").relativeTo(Paths.get("src")).toString())
        assertEquals("", Paths.get("dir").relativeTo(Paths.get("dir")).toString())
        assertEquals("..", Paths.get("dir").relativeTo(Paths.get("dir/subdir")).toString())
        assertEquals(Paths.get("../../test"), Paths.get("test").relativeTo(Paths.get("dir/dir")))
    }
}
