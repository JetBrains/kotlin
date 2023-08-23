/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import com.intellij.util.io.Compressor
import org.gradle.kotlin.dsl.support.unzipTo
import org.jetbrains.kotlin.gradle.utils.copyZipFilePartially
import org.jetbrains.kotlin.gradle.utils.listDescendants
import org.jetbrains.kotlin.util.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ZipUtilsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val zipContentFolder by lazy {
        temporaryFolder.newFolder().apply {
            resolve("stub0.txt").apply {
                parentFile.mkdirs()
                writeText("stub0 content")
                setLastModified(2411)
            }

            resolve("a/stub1.txt").apply {
                parentFile.mkdirs()
                writeText("stub1 content")
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
        return temporaryFolder.newFile().apply {
            Compressor.Zip(this).use { compressor -> compressor.addDirectory(directory) }
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
        val copiedFile = temporaryFolder.newFile()
        copyZipFilePartially(zipFile, copiedFile, path = "")
        assertZipContentEquals(
            temporaryFolder, zipContentFolder, copiedFile,
            "Expected correct content for copied file with root path"
        )
    }

    @Test
    fun `test copyZipFilePartially - a`() {
        val copiedFile = temporaryFolder.newFile()
        copyZipFilePartially(zipFile, copiedFile, path = "a/")
        assertZipContentEquals(
            temporaryFolder, zipContentFolder.resolve("a"), copiedFile,
            "Expected correct content for copied file with path 'a/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - a b`() {
        val copiedFile = temporaryFolder.newFile()
        copyZipFilePartially(zipFile, copiedFile, path = "a/b/")
        assertZipContentEquals(
            temporaryFolder, zipContentFolder.resolve("a/b"), copiedFile,
            "Expected correct content for copied file with path 'a/b/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - c`() {
        val copiedFile = temporaryFolder.newFile()
        copyZipFilePartially(zipFile, copiedFile, path = "c/")
        assertZipContentEquals(
            temporaryFolder, zipContentFolder.resolve("c"), copiedFile,
            "Expected correct content for copied file with path 'c/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - retains folder entries`() {
        val sourceZipFile = temporaryFolder.newFile()
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

        val destinationZipFile = temporaryFolder.newFile()
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
        val root1 = temporaryFolder.newFile()
        val root2 = temporaryFolder.newFile()

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
                        originalEntry.compressedSize, copyEntry.compressedSize,
                        "Expected same compressedSize on entry ${originalEntry.name}"
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
}

fun assertZipContentEquals(
    temporaryFolder: TemporaryFolder,
    expectedContent: File, zipFile: File, message: String
) {
    val zipOutputDirectory = temporaryFolder.newFolder()
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

