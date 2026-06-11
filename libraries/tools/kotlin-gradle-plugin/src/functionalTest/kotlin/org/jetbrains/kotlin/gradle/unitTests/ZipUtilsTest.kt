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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ZipUtilsTest : WithTemporaryFolder {
    @field:TempDir
    override lateinit var temporaryFolder: Path

    private val zipContentFolder by lazy {
        newTempDirectory().also { root ->
            root.resolve("stub0.txt").writeZipFixture("stub0 content\n", 2411)
            root.resolve("a/stub1.txt").writeZipFixture("stub1 content\n", 2412)
            root.resolve("a/b/stub2.txt").writeZipFixture("stub2 content", 2413)
            root.resolve("a/b/stub3.txt").writeZipFixture("stub3 content", 2414)
            root.resolve("c/stub4.txt").writeZipFixture("stub4 content", 2415)
        }.toFile()
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
        val copiedFile = newTempFile()
        copyZipFilePartially(zipFile.toPath(), copiedFile, path = "")
        assertZipContentEquals(
            zipContentFolder, copiedFile.toFile(),
            "Expected correct content for copied file with root path"
        )
    }

    @Test
    fun `test copyZipFilePartially - a`() {
        val copiedFile = newTempFile()
        copyZipFilePartially(zipFile.toPath(), copiedFile, path = "a/")
        assertZipContentEquals(
            zipContentFolder.resolve("a"), copiedFile.toFile(),
            "Expected correct content for copied file with path 'a/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - a b`() {
        val copiedFile = newTempFile()
        copyZipFilePartially(zipFile.toPath(), copiedFile, path = "a/b/")
        assertZipContentEquals(
            zipContentFolder.resolve("a/b"), copiedFile.toFile(),
            "Expected correct content for copied file with path 'a/b/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - c`() {
        val copiedFile = newTempFile()
        copyZipFilePartially(zipFile.toPath(), copiedFile, path = "c/")
        assertZipContentEquals(
            zipContentFolder.resolve("c"), copiedFile.toFile(),
            "Expected correct content for copied file with path 'c/'"
        )
    }

    @Test
    fun `test copyZipFilePartially - retains folder entries`() {
        val sourceZipFile = newTempFile()
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

        val destinationZipFile = newTempFile()
        copyZipFilePartially(sourceZipFile, destinationZipFile, "a/")

        ZipFile(destinationZipFile.toFile()).use { zip ->
            assertEquals(
                setOf("b/", "b/c/", "b/c/stub.txt"),
                zip.entries().toList().map { it.name }.sorted().toSet()
            )
        }
    }

    @Test
    fun `test copyZipFilePartially - creates exact same output file`() {
        val root1 = newTempFile()
        val root2 = newTempFile()

        copyZipFilePartially(zipFile.toPath(), root1, "")
        copyZipFilePartially(zipFile.toPath(), root2, "")

        assertTrue(
            Files.readAllBytes(root1).contentEquals(Files.readAllBytes(root2)),
            "Expected output zip files to be identical"
        )

        ZipFile(zipFile).use { original ->
            ZipFile(root1.toFile()).use { copy ->
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
        val sourceFile = newTempFile()
        Compressor.Zip(sourceFile)
            .withLevel(level)
            .use { compressor -> compressor.addDirectory(zipContentFolder.toPath()) }

        val allDirectoriesInZip = ZipFile(sourceFile.toFile()).use { it.entries().toList().filter { it.isDirectory }.map { it.name } }

        assertEquals(setOf("a/", "a/b/", "c/"), allDirectoriesInZip.toSet())

        // try to extract each directory in isolated env, check that content matches
        for (directory in allDirectoriesInZip + "") { // "" -- means root
            val destZip = newTempFile()
            copyZipFilePartially(sourceFile, destZip, directory)

            assertZipContentEquals(
                zipContentFolder.resolve(directory),
                destZip.toFile(),
                "Expected the same content for '${directory.ifEmpty { "<root>" }}'"
            )
        }
    }
}

private fun Path.writeZipFixture(content: String, lastModifiedMillis: Long) {
    parent?.let { Files.createDirectories(it) }
    Files.write(this, content.toByteArray())
    Files.setLastModifiedTime(this, FileTime.fromMillis(lastModifiedMillis))
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
