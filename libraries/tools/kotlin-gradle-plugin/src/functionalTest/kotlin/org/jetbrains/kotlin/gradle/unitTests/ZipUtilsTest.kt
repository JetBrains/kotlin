/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import com.intellij.util.io.Compressor
import org.gradle.kotlin.dsl.support.unzipTo
import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.testing.newTempDirectory
import org.jetbrains.kotlin.gradle.testing.newTempFile
import org.jetbrains.kotlin.gradle.utils.copyZipFilePartially
import org.jetbrains.kotlin.gradle.utils.listDescendants
import org.jetbrains.kotlin.util.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ZipUtilsTest : WithTemporaryFolder {
    @field:TempDir
    override lateinit var temporaryFolder: Path

    private val zipContentFolder by lazy {
        newTempDirectory().toFile().apply {
            resolve("stub0.txt").apply {
                parentFile.mkdirs()
                writeText("stub0 content\n")
                setLastModified(2411)
            }

            resolve("a/stub1.txt").apply {
                parentFile.mkdirs()
                writeText("stub1 content\n")
                setLastModified(2412)
            }

            resolve("a/b/stub2.txt").apply {
                parentFile.mkdirs()
                writeText("stub2 content")
                setLastModified(2413)
            }

            resolve("a/b/stub3.txt").apply {
                parentFile.mkdirs()
                writeText("stub3 content")
                setLastModified(2414)
            }

            resolve("c/stub4.txt").apply {
                parentFile.mkdirs()
                writeText("stub4 content")
                setLastModified(2415)
            }
        }
    }

    private fun zipDirectory(directory: File): File {
        return newTempFile().toFile().apply {
            Compressor.Zip(this.toPath()).use { compressor -> compressor.addDirectory(directory.toPath()) }
        }
    }

    private val zipFile by lazy {
        zipDirectory(zipContentFolder)
    }

    @Test
    fun `test - listDescendants - malformed path string`() {
        ZipFile(zipFile).use { zip ->
            assertThrows<IllegalArgumentException> { zip.listDescendants("a") }
            assertThrows<IllegalArgumentException> { zip.listDescendants("a/b") }
            assertThrows<IllegalArgumentException> { zip.listDescendants("c") }
        }
    }

    @Test
    fun `test listDescendants`() {
        ZipFile(zipFile).use { zip ->
            assertEquals(
                setOf("stub0.txt", "a/stub1.txt", "a/b/stub2.txt", "a/b/stub3.txt", "c/stub4.txt").sorted().toSet(),
                zip.listDescendants("").filter { it.isDirectory.not() }.map { it.name }.sorted().toSet(),
                "Expected all descendants being listed"
            )

            assertEquals(
                setOf("a/stub1.txt", "a/b/stub2.txt", "a/b/stub3.txt").sorted().toSet(),
                zip.listDescendants("a/").filter { it.isDirectory.not() }.map { it.name }.sorted().toSet(),
                "Expected all children of 'a/' being listed"
            )

            assertEquals(
                setOf("a/b/stub2.txt", "a/b/stub3.txt").sorted().toSet(),
                zip.listDescendants("a/b/").filter { it.isDirectory.not() }.map { it.name }.sorted().toSet(),
                "Expected all descendants of 'a/b/' being listed"
            )

            assertEquals(
                setOf("c/stub4.txt"),
                zip.listDescendants("c/").filter { it.isDirectory.not() }.map { it.name }.sorted().toSet(),
                "Expected all descendants of 'c/' being listed"
            )
        }
    }

    @Test
    fun `test copyZipFilePartially - root path`() {
        val copiedFile = newTempFile().toFile()
        copyZipFilePartially(zipFile, copiedFile, path = "")
        assertZipContentEquals(
            zipContentFolder, copiedFile,
            "Expected correct content for copied file with root path"
        )
    }

    @Test
    fun `test copyZipFilePartially - a`() {
        val copiedFile = newTempFile().toFile()
        copyZipFilePartially(zipFile, copiedFile, path = "a/")
        assertZipContentEquals(
            zipContentFolder.resolve("a"), copiedFile,
            "Expected correct content for copied file with path 'a/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - a b`() {
        val copiedFile = newTempFile().toFile()
        copyZipFilePartially(zipFile, copiedFile, path = "a/b/")
        assertZipContentEquals(
            zipContentFolder.resolve("a/b"), copiedFile,
            "Expected correct content for copied file with path 'a/b/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - c`() {
        val copiedFile = newTempFile().toFile()
        copyZipFilePartially(zipFile, copiedFile, path = "c/")
        assertZipContentEquals(
            zipContentFolder.resolve("c"), copiedFile,
            "Expected correct content for copied file with path 'c/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - retains folder entries`() {
        val sourceZipFile = newTempFile().toFile()
        ZipOutputStream(sourceZipFile.outputStream()).use { zipOutputStream ->
            zipOutputStream.putNextEntry(ZipEntry("a/"))
            zipOutputStream.closeEntry()

            zipOutputStream.putNextEntry(ZipEntry("a/b/"))
            zipOutputStream.closeEntry()

            zipOutputStream.putNextEntry(ZipEntry("a/b/c/"))
            zipOutputStream.closeEntry()

            zipOutputStream.putNextEntry(ZipEntry("a/b/c/stub.txt"))
            zipOutputStream.write("Text".toByteArray())
            zipOutputStream.closeEntry()
        }

        val destinationZipFile = newTempFile().toFile()
        copyZipFilePartially(sourceZipFile, destinationZipFile, "a/")

        ZipFile(destinationZipFile).use { zip ->
            assertEquals(
                setOf("b/", "b/c/", "b/c/stub.txt"),
                zip.entries().toList().map { it.name }.sorted().toSet()
            )
        }
    }

    @Test
    fun `test copyZipFilePartially - creates exact same output file`() {
        val root1 = newTempFile().toFile()
        val root2 = newTempFile().toFile()

        copyZipFilePartially(zipFile, root1, "")
        copyZipFilePartially(zipFile, root2, "")

        assertTrue(
            root1.readBytes().contentEquals(root2.readBytes()),
            "Expected output zip files to be identical"
        )

        ZipFile(zipFile).use { original ->
            ZipFile(root1).use { copy ->
                original.entries().toList().forEach { originalEntry ->
                    val copyEntry = copy.getEntry(originalEntry.name) ?: fail("Missing entry in copy: ${originalEntry.name}")
                    assertEquals(
                        originalEntry.comment, copyEntry.comment,
                        "Expected same comment on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.size, copyEntry.size,
                        "Expected same size on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.crc, copyEntry.crc,
                        "Expected same crc on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.creationTime, copyEntry.creationTime,
                        "Expected same creationTime on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.time, copyEntry.time,
                        "Expected same time on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.lastAccessTime, copyEntry.lastAccessTime,
                        "Expected same lastAccessTime on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.lastModifiedTime, copyEntry.lastModifiedTime,
                        "Expected same lastModifiedTime on entry ${originalEntry.name}"
                    )

                    assertEquals(
                        originalEntry.method, copyEntry.method,
                        "Expected same method on entry ${originalEntry.name}"
                    )
                }
            }
        }
    }

    @Test
    fun `test copyZipPartially with 0 compression level for source`() = testWithCompressionLevel(0)

    @Test
    fun `test copyZipPartially with 9 compression level for source`() = testWithCompressionLevel(9)

    private fun testWithCompressionLevel(level: Int) {
        val sourceFile = newTempFile().toFile()
        Compressor.Zip(sourceFile.toPath())
            .withLevel(level)
            .use { compressor -> compressor.addDirectory(zipContentFolder.toPath()) }

        val allDirectoriesInZip = ZipFile(sourceFile).use { it.entries().toList().filter { it.isDirectory }.map { it.name } }

        assertEquals(setOf("a/", "a/b/", "c/"), allDirectoriesInZip.toSet())

        // try to extract each directory in isolated env, check that content matches
        for (directory in allDirectoriesInZip + "") { // "" -- means root
            val destZip = newTempFile().toFile()
            copyZipFilePartially(sourceFile, destZip, directory)

            assertZipContentEquals(
                zipContentFolder.resolve(directory),
                destZip,
                "Expected the same content for '${directory.ifEmpty { "<root>" }}'"
            )
        }
    }
}

fun WithTemporaryFolder.assertZipContentEquals(
    expectedContent: File, zipFile: File, message: String,
) {
    val zipOutputDirectory = newTempDirectory().toFile()
    unzipTo(zipOutputDirectory, zipFile)
    assertDirectoryContentEquals(expectedContent, zipOutputDirectory, message)
}

fun assertDirectoryContentEquals(expected: File, actual: File, message: String) {
    assertTrue(expected.isDirectory, "Expected $expected to be directory")
    assertTrue(actual.isDirectory, "Expected $actual to be directory")

    val expectedFiles = expected.listFiles().orEmpty()
    val actualFiles = actual.listFiles().orEmpty()

    val expectedFileNames = expectedFiles.map { it.name }.sorted().toSet()
    val actualFileNames = actualFiles.map { it.name }.sorted().toSet()
    assertEquals(expectedFileNames, actualFileNames, "$message: ${expected.name} does not contain the same files")

    expectedFiles.forEach { expectedFile ->
        val actualFile = actualFiles.single { it.name == expectedFile.name }

        if (expectedFile.isFile && actualFile.isFile) {
            assertTrue(
                expectedFile.readBytes().contentEquals(actualFile.readBytes()),
                "$message: ${expectedFile.name} does not match in ${expected.name}"
            )
        } else if (expectedFile.isDirectory && actualFile.isDirectory) {
            assertDirectoryContentEquals(expectedFile, actualFile, message)
        } else {
            fail("Expected $expectedFile to be 'file' or 'directory'")
        }
    }
}
