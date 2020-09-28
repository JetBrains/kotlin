/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.nio.file.*
import kotlin.test.*

class PathExtensionsTest {

    @Test
    fun extension() {
        assertEquals("bbb", Paths.get("aaa.bbb").extension)
        assertEquals("", Paths.get("aaa").extension)
        assertEquals("", Paths.get("aaa.").extension)
        // maybe we should think that such files have name .bbb and no extension
        assertEquals("bbb", Paths.get(".bbb").extension)
        assertEquals("", Paths.get("/my.dir/log").extension)
    }

    @Test
    fun nameWithoutExtension() {
        assertEquals("aaa", Paths.get("aaa.bbb").nameWithoutExtension)
        assertEquals("aaa", Paths.get("aaa").nameWithoutExtension)
        assertEquals("aaa", Paths.get("aaa.").nameWithoutExtension)
        assertEquals("", Paths.get(".bbb").nameWithoutExtension)
        assertEquals("log", Paths.get("/my.dir/log").nameWithoutExtension)
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
            assertTrue(dstFile.listFiles().isEmpty(), "only directory is copied, but not its content")

            assertFailsWith(FileAlreadyExistsException::class, "copy dir do not overwrite dir") {
                srcFile.copyTo(dstFile)
            }

            srcFile.copyTo(dstFile, overwrite = true)
            assertTrue(dstFile.isDirectory())
            assertTrue(dstFile.listFiles().isEmpty(), "only directory is copied, but not its content")

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
        assertEquals(src.isFile(), dst.isFile(), message)
        if (dst.isFile()) {
            assertTrue(src.readBytes().contentEquals(dst.readBytes()), message)
        }
    }

    @Test
    fun testBufferedReader() {
        val file = Files.createTempFile(null, null)
        val lines = listOf("line1", "line2")
        Files.write(file, lines)

        assertEquals(file.bufferedReader().use { it.readLines() }, lines)
        assertEquals(file.bufferedReader(StandardOpenOption.READ).use { it.readLines() }, lines)
        assertEquals(file.bufferedReader(1024, StandardOpenOption.READ).use { it.readLines() }, lines)
        assertEquals(file.bufferedReader(Charsets.UTF_8, StandardOpenOption.READ).use { it.readLines() }, lines)
        assertEquals(file.bufferedReader(Charsets.UTF_8, 1024, StandardOpenOption.READ).use { it.readLines() }, lines)
    }

    @Test
    fun testBufferedWriter() {
        val file = Files.createTempFile(null, null)

        file.bufferedWriter().use { it.write("line1\n") }
        file.bufferedWriter(StandardOpenOption.APPEND).use { it.write("line2\n") }
        file.bufferedWriter(Charsets.UTF_8, StandardOpenOption.APPEND).use { it.write("line3\n") }
        file.bufferedWriter(1024, StandardOpenOption.APPEND).use { it.write("line4\n") }
        file.bufferedWriter(Charsets.UTF_8, 1024, StandardOpenOption.APPEND).use { it.write("line5\n") }

        assertEquals(Files.readAllLines(file), listOf("line1", "line2", "line3", "line4", "line5"))
    }

    @Test
    fun testPrintWriter() {
        val file = Files.createTempFile(null, null)

        val writer = file.printWriter()
        val str1 = "Hello, world!"
        val str2 = "Everything is wonderful!"
        writer.println(str1)
        writer.println(str2)
        writer.close()

        val writer2 = file.printWriter(StandardOpenOption.APPEND)
        val str3 = "Hello again!"
        writer2.println(str3)
        writer2.close()

        val writer3 = file.printWriter(Charsets.UTF_8, StandardOpenOption.APPEND)
        val str4 = "Hello one last time!"
        writer3.println(str4)
        writer3.close()

        val reader = file.bufferedReader()
        assertEquals(str1, reader.readLine())
        assertEquals(str2, reader.readLine())
        assertEquals(str3, reader.readLine())
        assertEquals(str4, reader.readLine())
    }

    @Test
    fun testWriteBytes() {
        val file = Files.createTempFile(null, null)
        file.writeBytes("Hello".encodeToByteArray())
        file.appendBytes(" world!".encodeToByteArray())
        assertEquals(file.readText(), "Hello world!")
    }

    @Test
    fun testAttributeGetters() {
        val file = Files.createTempFile(null, null)
        assertTrue(file.exists())
        assertTrue(file.isFile())
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
    fun testListFiles() {
        val dir = Files.createTempDirectory(null)
        assertEquals(dir.listFiles().size, 0)

        val file = dir.resolve("f1")
        Files.createFile(file)
        assertEquals(dir.listFiles().size, 1)

        assertFailsWith<NotDirectoryException> { file.listFiles() }
    }
}
