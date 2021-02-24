/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.*
import kotlin.io.path.*
import kotlin.random.Random
import kotlin.test.*

class PathExtensionsTest : AbstractPathTest() {
    private val isCaseInsensitiveFileSystem = Path("C:/") == Path("c:/")
    private val isBackslashSeparator = FileSystems.getDefault().separator == "\\"

    @Test
    fun filenameComponents() {
        fun check(path: String, name: String, nameNoExt: String, extension: String) {
            val p = Path(path)
            assertEquals(name, p.name, "name")
            assertEquals(nameNoExt, p.nameWithoutExtension, "nameWithoutExtension")
            assertEquals(extension, p.extension, "extension")
        }

        check(path = "aaa.bbb", name = "aaa.bbb", nameNoExt = "aaa", extension = "bbb")
        check(path = "aaa", name = "aaa", nameNoExt = "aaa", extension = "")
        check(path = "aaa.", name = "aaa.", nameNoExt = "aaa", extension = "")
        check(path = ".aaa", name = ".aaa", nameNoExt = "", extension = "aaa")
        check(path = "/dir.ext/aaa.bbb", name = "aaa.bbb", nameNoExt = "aaa", extension = "bbb")
        check(path = "/dir.ext/aaa", name = "aaa", nameNoExt = "aaa", extension = "")
        check(path = "/", name = "", nameNoExt = "", extension = "")
        check(path = "", name = "", nameNoExt = "", extension = "")
    }

    @Test
    fun invariantSeparators() {
        val path = Path("base") / "nested" / "leaf"
        assertEquals("base/nested/leaf", path.invariantSeparatorsPathString)

        val path2 = Path("base", "nested", "terminal")
        assertEquals("base/nested/terminal", path2.invariantSeparatorsPathString)
    }

    @Test
    fun createNewFile() {
        val dir = createTempDirectory().cleanupRecursively()

        val file = dir / "new-file"

        assertTrue(file.notExists())

        file.createFile()
        assertTrue(file.exists())
        assertTrue(file.isRegularFile())

        assertFailsWith<FileAlreadyExistsException> { file.createFile() }
    }

    @Test
    fun createTempFileDefaultDir() {
        val file1 = createTempFile().cleanup()
        val file2 = createTempFile(directory = null).cleanup()

        assertEquals(file1.parent, file2.parent)
    }

    @Test
    fun createTempDirectoryDefaultDir() {
        val dir1 = createTempDirectory().cleanup()
        val dir2 = createTempDirectory(directory = null).cleanupRecursively()
        val dir3 = createTempDirectory(dir2)

        assertEquals(dir1.parent, dir2.parent)
        assertNotEquals(dir2.parent, dir3.parent)
    }

    @Test
    fun copyTo() {
        val root = createTempDirectory("copyTo-root").cleanupRecursively()
        val srcFile = createTempFile(root, "src")
        val dstFile = createTempFile(root, "dst")

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

        assertTrue(dstFile.deleteIfExists())
        dst = srcFile.copyTo(dstFile)
        compareFiles(srcFile, dst, "copy to new file")

        val subDst = dstFile.resolve("foo/bar")
        assertFailsWith<FileSystemException> { srcFile.copyTo(subDst) }
        assertFailsWith<FileSystemException> { srcFile.copyTo(subDst, overwrite = true) }
        assertTrue(dstFile.deleteIfExists())
        assertFailsWith<FileSystemException> { srcFile.copyTo(subDst) }

        dstFile.createDirectory()
        val child = dstFile.resolve("child").createFile()
        assertFailsWith<DirectoryNotEmptyException>("copy with overwrite do not overwrite non-empty dir") {
            srcFile.copyTo(dstFile, overwrite = true)
        }
        child.deleteExisting()

        srcFile.copyTo(dstFile, overwrite = true)
        assertEquals(srcFile.readText(), dstFile.readText(), "copy with overwrite over empty dir")

        assertTrue(srcFile.deleteIfExists())
        assertTrue(dstFile.deleteIfExists())

        assertFailsWith<NoSuchFileException> {
            srcFile.copyTo(dstFile)
        }

        srcFile.createDirectory()
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
    }

    @Test
    fun copyToNameWithoutParent() {
        val currentDir = Path("").absolute()
        val srcFile = createTempFile().cleanup()
        val dstFile = createTempFile(directory = currentDir).cleanup()

        srcFile.writeText("Hello, World!", Charsets.UTF_8)
        dstFile.deleteExisting()

        val dstRelative = Path(dstFile.name)

        srcFile.copyTo(dstRelative)

        assertEquals(srcFile.readText(), dstFile.readText())
    }

    @Test
    fun moveTo() {
        val root = createTempDirectory("moveTo-root").cleanupRecursively()
        val original = createTempFile(root, "original")
        val srcFile = createTempFile(root, "src")
        val dstFile = createTempFile(root, "dst")
        fun restoreSrcFile() { original.copyTo(srcFile, overwrite = true) }
        original.writeText("Hello, World!")
        restoreSrcFile()

        assertFailsWith<FileAlreadyExistsException>("do not overwrite existing file") {
            srcFile.moveTo(dstFile)
        }

        var dst = srcFile.moveTo(dstFile, overwrite = true)
        assertSame(dst, dstFile)
        compareFiles(original, dst, "move with overwrite over existing file")
        assertTrue(srcFile.notExists())

        restoreSrcFile()
        srcFile.moveTo(srcFile)
        srcFile.moveTo(srcFile, overwrite = true)

        compareFiles(original, srcFile, "move file to itself leaves it intact")

        assertTrue(dstFile.deleteIfExists())
        dst = srcFile.moveTo(dstFile)
        compareFiles(original, dst, "move to new file")

        restoreSrcFile()
        val subDst = dstFile.resolve("foo/bar")
        assertFailsWith<FileSystemException> { srcFile.moveTo(subDst) }
        assertFailsWith<FileSystemException> { srcFile.moveTo(subDst, overwrite = true) }
        assertTrue(dstFile.deleteIfExists())
        assertFailsWith<FileSystemException> { srcFile.moveTo(subDst) }

        dstFile.createDirectory()
        val child = dstFile.resolve("child").createFile()
        assertFailsWith<DirectoryNotEmptyException>("move with overwrite do not overwrite non-empty dir") {
            srcFile.moveTo(dstFile, overwrite = true)
        }
        child.deleteExisting()

        srcFile.moveTo(dstFile, overwrite = true)
        compareFiles(original, dstFile, "move with overwrite over empty dir")

        assertTrue(srcFile.notExists())
        assertTrue(dstFile.deleteIfExists())

        assertFailsWith<NoSuchFileException> {
            srcFile.moveTo(dstFile)
        }

        srcFile.createDirectory()
        srcFile.resolve("somefile").writeText("some content")
        dstFile.writeText("")
        assertFailsWith<FileAlreadyExistsException>("move dir do not overwrite file") {
            srcFile.moveTo(dstFile)
        }
        srcFile.moveTo(dstFile, overwrite = true)
        assertTrue(dstFile.isDirectory())
        assertEquals(listOf(dstFile / "somefile"), dstFile.listDirectoryEntries(), "directory is moved with its content")
    }

    private fun compareFiles(src: Path, dst: Path, message: String? = null) {
        assertTrue(dst.exists())
        assertEquals(src.isRegularFile(), dst.isRegularFile(), message)
        assertEquals(src.isDirectory(), dst.isDirectory(), message)
        if (dst.isRegularFile()) {
            assertTrue(src.readBytes().contentEquals(dst.readBytes()), message)
        }
    }

    @Test
    fun fileSize() {
        val file = createTempFile().cleanup()
        assertEquals(0, file.fileSize())

        file.writeBytes(ByteArray(100))
        assertEquals(100, file.fileSize())

        file.appendText("Hello", Charsets.US_ASCII)
        assertEquals(105, file.fileSize())

        file.deleteExisting()
        assertFailsWith<NoSuchFileException> { file.fileSize() }
    }

    @Test
    fun deleteExisting() {
        val file = createTempFile().cleanup()
        file.deleteExisting()
        assertFailsWith<NoSuchFileException> { file.deleteExisting() }

        val dir = createTempDirectory().cleanup()
        dir.deleteExisting()
        assertFailsWith<NoSuchFileException> { dir.deleteExisting() }
    }

    @Test
    fun deleteIfExists() {
        val file = createTempFile().cleanup()
        assertTrue(file.deleteIfExists())
        assertFalse(file.deleteIfExists())

        val dir = createTempDirectory().cleanup()
        assertTrue(dir.deleteIfExists())
        assertFalse(dir.deleteIfExists())
    }

    @Test
    fun attributeGettersOnFile() {
        val file = createTempFile("temp", ".file").cleanup()
        assertTrue(file.exists())
        assertFalse(file.notExists())
        assertTrue(file.isRegularFile())
        assertFalse(file.isDirectory())
        assertFalse(file.isSymbolicLink())
        assertTrue(file.isReadable())
        assertTrue(file.isWritable())
        assertTrue(file.isSameFileAs(file))

        // The default value of these depends on the current operating system, so just check that
        // they don't throw an exception.
        file.isExecutable()
        file.isHidden()
    }

    @Test
    fun attributeGettersOnDirectory() {
        val file = createTempDirectory(".tmpdir").cleanup()
        assertTrue(file.exists())
        assertFalse(file.notExists())
        assertFalse(file.isRegularFile())
        assertTrue(file.isDirectory())
        assertFalse(file.isSymbolicLink())
        assertTrue(file.isReadable())
        assertTrue(file.isWritable())
        assertTrue(file.isSameFileAs(file))

        file.isExecutable()
        file.isHidden()
    }

    @Test
    fun attributeGettersOnNonExistentPath() {
        val file = createTempDirectory().cleanup().resolve("foo")
        assertFalse(file.exists())
        assertTrue(file.notExists())
        assertFalse(file.isRegularFile())
        assertFalse(file.isDirectory())
        assertFalse(file.isSymbolicLink())
        assertFalse(file.isReadable())
        assertFalse(file.isWritable())
        assertTrue(file.isSameFileAs(file))

        file.isExecutable()
        // This function will either throw an exception or return false,
        // depending on the operating system.
        try {
            assertFalse(file.isHidden())
        } catch (e: IOException) {
        }
    }

    private interface SpecialFileAttributesView : FileAttributeView
    private interface SpecialFileAttributes : BasicFileAttributes

    @Test
    fun readWriteAttributes() {
        val file = createTempFile().cleanup()
        val modifiedTime = file.getLastModifiedTime()
        assertEquals(modifiedTime, file.getAttribute("lastModifiedTime"))
        assertEquals(modifiedTime, file.getAttribute("basic:lastModifiedTime"))
        assertEquals(modifiedTime, file.readAttributes<BasicFileAttributes>().lastModifiedTime())
        assertEquals(modifiedTime, file.readAttributes("basic:lastModifiedTime,creationTime")["lastModifiedTime"])
        assertEquals(modifiedTime, file.readAttributes("*")["lastModifiedTime"])

        assertFailsWith<UnsupportedOperationException> { file.readAttributes<SpecialFileAttributes>() }
        assertFailsWith<UnsupportedOperationException> { file.readAttributes("really_unsupported_view:*") }
        assertFailsWith<IllegalArgumentException> { file.readAttributes("basic:really_unknown_attribute") }

        val newTime1 = FileTime.fromMillis(modifiedTime.toMillis() + 3600_000)
        file.setLastModifiedTime(newTime1)
        assertEquals(newTime1, file.getLastModifiedTime())

        val newTime2 = FileTime.fromMillis(modifiedTime.toMillis() + 2 * 3600_000)
        file.setAttribute("lastModifiedTime", newTime2)
        assertEquals(newTime2, file.getLastModifiedTime())

        val newTime3 = FileTime.fromMillis(modifiedTime.toMillis() + 3 * 3600_000)
        file.fileAttributesView<BasicFileAttributeView>().setTimes(newTime3, null, null)
        assertEquals(newTime3, file.getLastModifiedTime())

        assertFailsWith<UnsupportedOperationException> { file.fileAttributesView<SpecialFileAttributesView>() }
        assertNull(file.fileAttributesViewOrNull<SpecialFileAttributesView>())

        file.setAttribute("lastModifiedTime", null)
        assertEquals(newTime3, file.getLastModifiedTime())
    }

    @Test
    fun links() {
        val dir = createTempDirectory().cleanupRecursively()
        val original = createTempFile(dir)
        original.writeBytes(Random.nextBytes(100))

        val link = try {
            (dir / ("link-" + original.fileName)).createLinkPointingTo(original)
        } catch (e: IOException) {
            // may require a privilege
            println("Creating a link failed with ${e.stackTraceToString()}")
            return
        }

        assertTrue(link.isRegularFile())
        assertTrue(link.isRegularFile(LinkOption.NOFOLLOW_LINKS))
        assertTrue(original.isSameFileAs(link))
        compareFiles(original, link)
        assertFailsWith<NotLinkException> { link.readSymbolicLink() }
    }

    @Test
    fun symlinks() {
        val dir = createTempDirectory().cleanupRecursively()
        val original = createTempFile(dir)
        original.writeBytes(Random.nextBytes(100))

        val symlink = try {
            (dir / ("symlink-" + original.fileName)).createSymbolicLinkPointingTo(original)
        } catch (e: IOException) {
            // may require a privilege
            println("Creating a symlink failed with ${e.stackTraceToString()}")
            return
        }

        assertTrue(symlink.isRegularFile())
        assertFalse(symlink.isRegularFile(LinkOption.NOFOLLOW_LINKS))
        assertTrue(original.isSameFileAs(symlink))
        compareFiles(original, symlink)
        assertEquals(original, symlink.readSymbolicLink())
    }

    @Test
    fun directoryEntriesList() {
        val dir = createTempDirectory().cleanupRecursively()
        assertEquals(0, dir.listDirectoryEntries().size)

        val file = dir.resolve("f1").createFile()
        assertEquals(listOf(file), dir.listDirectoryEntries())

        val fileTxt = createTempFile(dir, suffix = ".txt")
        assertEquals(listOf(fileTxt), dir.listDirectoryEntries("*.txt"))

        assertFailsWith<NotDirectoryException> { file.listDirectoryEntries() }
    }

    @Test
    fun directoryEntriesUseSequence() {
        val dir = createTempDirectory().cleanupRecursively()
        assertEquals(0, dir.useDirectoryEntries { it.toList() }.size)

        val file = dir.resolve("f1").createFile()
        assertEquals(listOf(file), dir.useDirectoryEntries { it.toList() })

        val fileTxt = createTempFile(dir, suffix = ".txt")
        assertEquals(listOf(fileTxt), dir.useDirectoryEntries("*.txt") { it.toList() })

        assertFailsWith<NotDirectoryException> { file.useDirectoryEntries { error("shouldn't get here") } }
    }

    @Test
    fun directoryEntriesForEach() {
        val dir = createTempDirectory().cleanupRecursively()
        dir.forEachDirectoryEntry { error("shouldn't get here, but received $it") }

        val file = createTempFile(dir)
        dir.forEachDirectoryEntry { assertEquals(file, it) }

        val fileTxt = createTempFile(dir, suffix = ".txt")
        dir.forEachDirectoryEntry("*.txt") { assertEquals(fileTxt, it) }

        assertFailsWith<NotDirectoryException> { file.forEachDirectoryEntry { error("shouldn't get here, but received $it") } }
    }


    private fun testRelativeTo(expected: String?, path: String, base: String) =
        testRelativeTo(expected?.let { Path(it) }, Path(path), Path(base))
    private fun testRelativeTo(expected: String, path: Path, base: Path) =
        testRelativeTo(Path(expected), path, base)

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
        val nested = Path("foo/bar")
        val base = Path("foo")

        testRelativeTo("bar", nested, base)
        testRelativeTo("..", base, nested)

        val empty = Path("")
        val current = Path(".")
        val parent = Path("..")
        val outOfRoot = Path("../bar")

        testRelativeTo("../bar", outOfRoot, empty)
        testRelativeTo("../../bar", outOfRoot, base)
        testRelativeTo("bar", outOfRoot, parent)
        testRelativeTo("..", parent, outOfRoot)

        val root = Path("/root")
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
        val absolute = Path("/foo/bar/baz")
        val relative = Path("foo/bar")
        val networkShare1 = Path("""\\my.host\share1/folder""")
        val networkShare2 = Path("""\\my.host\share2\folder""")

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

    @Test
    fun absolutePaths() {
        val relative = Path("./example")
        assertTrue(relative.absolute().isAbsolute)
        assertEquals(relative.absolute().pathString, relative.absolutePathString())
    }
}
