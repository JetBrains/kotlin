/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.lang.NullPointerException
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.jdk7.test.PathTreeWalkTest.Companion.createTestFiles
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilenames
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilesOnly
import kotlin.jdk7.test.PathTreeWalkTest.Companion.testVisitedFiles
import kotlin.test.*

class PathRecursiveFunctionsTest : AbstractPathTest() {
    @Test
    fun deleteFile() {
        val file = createTempFile()

        assertTrue(file.exists())
        file.deleteRecursively()
        assertFalse(file.exists())

        file.createFile().writeText("non-empty file")

        assertTrue(file.exists())
        file.deleteRecursively()
        assertFalse(file.exists())
        file.deleteRecursively() // successfully deletes recursively a non-existent file
    }

    @Test
    fun deleteDirectory() {
        val dir = createTestFiles()

        assertTrue(dir.exists())
        dir.deleteRecursively()
        assertFalse(dir.exists())
        dir.deleteRecursively() // successfully deletes recursively a non-existent directory
    }

    @Test
    fun deleteNotExistingParent() {
        val basedir = createTempDirectory().cleanupRecursively()
        basedir.resolve("a/b").deleteRecursively()
        basedir.resolve("a/b/c").deleteRecursively()
    }

    private fun Path.walkIncludeDirectories(): Sequence<Path> =
        this.walk(PathWalkOption.INCLUDE_DIRECTORIES)

    @Test
    fun deleteRestrictedRead() {
        val basedir = createTestFiles().cleanupRecursively()
        val restrictedEmptyDir = basedir.resolve("6")
        val restrictedDir = basedir.resolve("1")
        val restrictedFile = basedir.resolve("7.txt")

        withRestrictedRead(restrictedEmptyDir, restrictedDir, restrictedFile) {
            val error = assertFailsWith<java.nio.file.FileSystemException>("Expected incomplete recursive deletion") {
                basedir.deleteRecursively()
            }

            // AccessDeniedException when opening restrictedEmptyDir and restrictedDir, wrapped in FileSystemException if SecureDirectoryStream was used
            // DirectoryNotEmptyException is not thrown from parent directory
            assertEquals(2, error.suppressedExceptions.size)
            assertIs<java.nio.file.AccessDeniedException>(error.suppressedExceptions[0].let { it.cause ?: it })
            assertIs<java.nio.file.AccessDeniedException>(error.suppressedExceptions[1].let { it.cause ?: it })

            // Couldn't read directory entries.
            // No attempt to delete even when empty directories can be removed without write permission
            assertTrue(restrictedEmptyDir.exists())
            assertTrue(restrictedDir.exists()) // couldn't read directory entries
            assertFalse(restrictedFile.exists()) // restricted read allows removal of file

            restrictedEmptyDir.toFile().setReadable(true)
            restrictedDir.toFile().setReadable(true)
            testVisitedFiles(listOf("", "1", "1/2", "1/3", "1/3/4.txt", "1/3/5.txt", "6"), basedir.walkIncludeDirectories(), basedir)
            basedir.deleteRecursively()
        }
    }

    @Test
    fun deleteRestrictedWrite() {
        val basedir = createTestFiles().cleanupRecursively()
        val restrictedEmptyDir = basedir.resolve("6")
        val restrictedDir = basedir.resolve("8")
        val restrictedFile = basedir.resolve("1/3/5.txt")

        withRestrictedWrite(restrictedEmptyDir, restrictedDir, restrictedFile) {
            val error = assertFailsWith<java.nio.file.FileSystemException>("Expected incomplete recursive deletion") {
                basedir.deleteRecursively()
            }

            // AccessDeniedException when deleting "8/9.txt", wrapped in FileSystemException if SecureDirectoryStream was used
            // DirectoryNotEmptyException is not thrown from parent directories
            when (val accessDenied = error.suppressedExceptions.single()) {
                is java.nio.file.AccessDeniedException -> {
                    assertEquals(restrictedDir.resolve("9.txt").toString(), accessDenied.file)
                }
                is java.nio.file.FileSystemException -> {
                    assertEquals(restrictedDir.resolve("9.txt").toString(), accessDenied.file)
                    assertIs<java.nio.file.AccessDeniedException>(accessDenied.cause)
                }
                else -> {
                    fail("Unexpected exception $accessDenied")
                }
            }

            assertFalse(restrictedEmptyDir.exists()) // empty directories can be removed without write permission
            assertTrue(restrictedDir.exists())
            assertTrue(restrictedDir.resolve("9.txt").exists())
            assertFalse(restrictedFile.exists()) // plain files can be removed without write permission
        }
    }

    @Test
    fun deleteBaseSymlinkToFile() {
        val file = createTempFile().cleanup()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(file) ?: return

        link.deleteRecursively()
        assertFalse(link.exists(LinkOption.NOFOLLOW_LINKS))
        assertTrue(file.exists())
    }

    @Test
    fun deleteBaseSymlinkToDirectory() {
        val dir = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(dir) ?: return

        link.deleteRecursively()
        assertFalse(link.exists(LinkOption.NOFOLLOW_LINKS))
        testVisitedFiles(listOf("") + referenceFilenames, dir.walkIncludeDirectories(), dir)
    }

    @Test
    fun deleteSymlinkToFile() {
        val file = createTempFile().cleanup()
        val dir = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(file) ?: return }

        dir.deleteRecursively()
        assertFalse(dir.exists())
        assertTrue(file.exists())
    }

    @Test
    fun deleteSymlinkToDirectory() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(dir1) ?: return }

        dir2.deleteRecursively()
        assertFalse(dir2.exists())
        testVisitedFiles(listOf("") + referenceFilenames, dir1.walkIncludeDirectories(), dir1)
    }

    @Test
    fun deleteParentSymlink() {
        val dir1 = createTestFiles().cleanupRecursively()
        val dir2 = createTempDirectory().cleanupRecursively().also { it.resolve("link").tryCreateSymbolicLinkTo(dir1) ?: return }

        dir2.resolve("link/8").deleteRecursively()
        assertFalse(dir1.resolve("8").exists())

        dir2.resolve("link/1/3").deleteRecursively()
        assertFalse(dir1.resolve("1/3").exists())
    }

    @Test
    fun deleteSymlinkToSymlink() {
        val dir = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(dir) ?: return
        val linkToLink = createTempDirectory().cleanupRecursively().resolve("linkToLink").tryCreateSymbolicLinkTo(link) ?: return

        linkToLink.deleteRecursively()
        assertFalse(linkToLink.exists(LinkOption.NOFOLLOW_LINKS))
        assertTrue(link.exists(LinkOption.NOFOLLOW_LINKS))
        testVisitedFiles(listOf("") + referenceFilenames, dir.walkIncludeDirectories(), dir)
    }

    @Test
    fun deleteSymlinkCyclic() {
        val basedir = createTestFiles().cleanupRecursively()
        val original = basedir.resolve("1")
        original.resolve("2/link").tryCreateSymbolicLinkTo(original) ?: return

        basedir.deleteRecursively()
        assertFalse(basedir.exists())
    }

    @Test
    fun deleteSymlinkCyclicWithTwo() {
        val basedir = createTestFiles().cleanupRecursively()
        val dir8 = basedir.resolve("8")
        val dir2 = basedir.resolve("1/2")
        dir8.resolve("linkTo2").tryCreateSymbolicLinkTo(dir2) ?: return
        dir2.resolve("linkTo8").tryCreateSymbolicLinkTo(dir8) ?: return

        basedir.deleteRecursively()
        assertFalse(basedir.exists())
    }

    @Test
    fun deleteSymlinkPointingToItself() {
        val basedir = createTempDirectory().cleanupRecursively()
        val link = basedir.resolve("link")
        link.tryCreateSymbolicLinkTo(link) ?: return

        basedir.deleteRecursively()
        assertFalse(basedir.exists())
    }

    @Test
    fun deleteSymlinkTwoPointingToEachOther() {
        val basedir = createTempDirectory().cleanupRecursively()
        val link1 = basedir.resolve("link1")
        val link2 = basedir.resolve("link2").tryCreateSymbolicLinkTo(link1) ?: return
        link1.tryCreateSymbolicLinkTo(link2) ?: return

        basedir.deleteRecursively()
        assertFalse(basedir.exists())
    }

    private fun compareFiles(src: Path, dst: Path, message: String? = null) {
        assertTrue(dst.exists())
        assertEquals(src.isRegularFile(), dst.isRegularFile(), message)
        assertEquals(src.isDirectory(), dst.isDirectory(), message)
        if (dst.isRegularFile()) {
            assertTrue(src.readBytes().contentEquals(dst.readBytes()), message)
        }
    }

    private fun compareDirectories(src: Path, dst: Path) {
        for (srcFile in src.walkIncludeDirectories()) {
            val dstFile = dst.resolve(srcFile.relativeTo(src))
            compareFiles(srcFile, dstFile)
        }
    }

    @Test
    fun copyFileToFile() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        val copyResult = src.copyToRecursively(dst, followLinks = false)
        assertEquals(dst, copyResult)
        compareFiles(src, dst)

        dst.writeText("bye")
        assertFailsWith<java.nio.file.FileAlreadyExistsException> {
            src.copyToRecursively(dst, followLinks = false)
        }
        assertEquals("bye", dst.readText())

        src.copyToRecursively(dst, followLinks = false, overwrite = true)
        compareFiles(src, dst)
    }

    @Test
    fun copyFileToDirectory() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val dst = createTestFiles().cleanupRecursively()

        assertFailsWith<java.nio.file.FileAlreadyExistsException> {
            src.copyToRecursively(dst, followLinks = false)
        }
        assertTrue(dst.isDirectory())

        assertFailsWith<java.nio.file.DirectoryNotEmptyException> {
            src.copyToRecursively(dst, followLinks = false) { source, target ->
                source.copyTo(target, overwrite = true)
                CopyActionResult.CONTINUE
            }
        }
        assertTrue(dst.isDirectory())

        val copyResult = src.copyToRecursively(dst, followLinks = false, overwrite = true)
        assertEquals(dst, copyResult)
        compareFiles(src, dst)
    }

    private fun Path.relativePathString(base: Path): String {
        return relativeToOrSelf(base).invariantSeparatorsPathString
    }

    @Test
    fun copyDirectoryToDirectory() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        val copyResult = src.copyToRecursively(dst, followLinks = false)
        assertEquals(dst, copyResult)
        compareDirectories(src, dst)

        src.resolve("1/3/4.txt").writeText("hello")
        dst.resolve("10").createDirectory()

        val conflictingFiles = mutableListOf<String>()
        src.copyToRecursively(dst, followLinks = false, onError = { source, _, exception ->
            assertIs<java.nio.file.FileAlreadyExistsException>(exception)
            conflictingFiles.add(source.relativePathString(src))
            OnErrorResult.SKIP_SUBTREE
        })
        assertEquals(referenceFilesOnly.sorted(), conflictingFiles.sorted())
        assertTrue(dst.resolve("1/3/4.txt").readText().isEmpty())

        src.copyToRecursively(dst, followLinks = false, overwrite = true)
        compareDirectories(src, dst)
        assertTrue(dst.resolve("10").exists())
    }

    @Test
    fun copyDirectoryToFile() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempFile().cleanupRecursively().also { it.writeText("hello") }

        val existsException = assertFailsWith<java.nio.file.FileAlreadyExistsException> {
            src.copyToRecursively(dst, followLinks = false)
        }
        // attempted to copy only the root directory(src)
        assertEquals(dst.toString(), existsException.file)
        assertTrue(dst.isRegularFile())

        src.copyToRecursively(dst, followLinks = false, overwrite = true)
        compareDirectories(src, dst)
    }

    @Test
    fun copyNonExistentSource() {
        val src = createTempDirectory().also { it.deleteExisting() }
        val dst = createTempDirectory()

        assertFailsWith<java.nio.file.NoSuchFileException> {
            src.copyToRecursively(dst, followLinks = false)
        }

        dst.deleteExisting()
        assertFailsWith<java.nio.file.NoSuchFileException> {
            src.copyToRecursively(dst, followLinks = false)
        }
    }

    @Test
    fun copyNonExistentDestinationParent() {
        val src = createTempDirectory().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("parent/dst")

        assertFalse(dst.parent.exists())

        src.copyToRecursively(dst, followLinks = false, onError = { source, target, exception ->
            assertIs<java.nio.file.NoSuchFileException>(exception)
            assertEquals(src, source)
            assertEquals(dst, target)
            assertEquals(dst.toString(), exception.file)
            OnErrorResult.SKIP_SUBTREE
        })

        src.copyToRecursively(dst.createParentDirectories(), followLinks = false)
    }

    @Test
    fun copyRestrictedReadInSource() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively()

        val restrictedDir = src.resolve("1/3")
        val restrictedFile = src.resolve("7.txt")

        withRestrictedRead(restrictedDir, restrictedFile, alsoReset = listOf(dst.resolve("1/3"), dst.resolve("7.txt"))) {
            // Restricted directories fail during traversal, while files fail when copied.
            // Because Files.walkFileTree opens a directory before calling FileVisitor.onPreVisitDirectory with it.
            src.copyToRecursively(dst, followLinks = false, onError = { source, _, exception ->
                assertIs<java.nio.file.AccessDeniedException>(exception)
                assertEquals(source.toString(), exception.file)
                assertEquals("1/3", source.relativePathString(src))
                OnErrorResult.SKIP_SUBTREE
            }) { source, target ->
                try {
                    source.copyToIgnoringExistingDirectory(target, followLinks = false)
                } catch (exception: Throwable) {
                    assertIs<java.nio.file.AccessDeniedException>(exception)
                    assertEquals(source.toString(), exception.file)
                    assertEquals("7.txt", source.relativePathString(src))
                }
                CopyActionResult.CONTINUE
            }

            assertFalse(dst.resolve("1/3").exists()) // restricted directory is not copied
            assertFalse(dst.resolve("7.txt").exists()) // restricted file is not copied
        }
    }

    @Test
    fun copyRestrictedWriteInSource() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively()

        val restrictedDir = src.resolve("1/3")
        val restrictedFile = src.resolve("7.txt")

        withRestrictedWrite(restrictedDir, restrictedFile, alsoReset = listOf(dst.resolve("1/3"), dst.resolve("7.txt"))) {
            val accessDeniedFiles = mutableListOf<String>()
            src.copyToRecursively(dst, followLinks = false, onError = { _, target, exception ->
                assertIs<java.nio.file.AccessDeniedException>(exception)
                assertEquals(target.toString(), exception.file)
                accessDeniedFiles.add(target.relativePathString(dst))
                OnErrorResult.SKIP_SUBTREE
            })
            assertEquals(listOf("1/3/4.txt", "1/3/5.txt"), accessDeniedFiles.sorted())

            assertTrue(dst.resolve("1/3").exists()) // restricted directory is copied
            assertFalse(dst.resolve("1/3").isWritable()) // access permissions are copied
            assertTrue(dst.resolve("7.txt").exists()) // restricted file is copied
            assertFalse(dst.resolve("7.txt").isWritable()) // access permissions are copied
        }
    }

    @Test
    fun copyRestrictedWriteInDestination() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTestFiles().cleanupRecursively()

        src.resolve("1/3/4.txt").writeText("hello")
        src.resolve("7.txt").writeText("hi")

        val restrictedDir = dst.resolve("1/3")
        val restrictedFile = dst.resolve("7.txt")

        withRestrictedWrite(restrictedDir, restrictedFile) {
            val accessDeniedFiles = mutableListOf<String>()
            src.copyToRecursively(dst, followLinks = false, overwrite = true, onError = { _, target, exception ->
                assertIs<java.nio.file.AccessDeniedException>(exception)
                assertEquals(target.toString(), exception.file)
                accessDeniedFiles.add(target.relativePathString(dst))
                OnErrorResult.SKIP_SUBTREE
            })
            assertEquals(listOf("1/3/4.txt", "1/3/5.txt"), accessDeniedFiles.sorted())

            assertNotEquals(src.resolve("1/3/4.txt").readText(), dst.resolve("1/3/4.txt").readText())
            assertEquals(src.resolve("7.txt").readText(), dst.resolve("7.txt").readText())
        }
    }

    @Test
    fun copyBrokenBaseSymlink() {
        val basedir = createTempDirectory().cleanupRecursively()
        val target = basedir.resolve("target")
        val link = basedir.resolve("link").tryCreateSymbolicLinkTo(target) ?: return
        val dst = basedir.resolve("dst")

        // the same behavior as link.copyTo(dst, LinkOption.NOFOLLOW_LINKS)
        link.copyToRecursively(dst, followLinks = false)
        assertTrue(dst.isSymbolicLink())
        assertTrue(dst.exists(LinkOption.NOFOLLOW_LINKS))
        assertFalse(dst.exists())

        assertFailsWith<java.nio.file.FileAlreadyExistsException> {
            link.copyToRecursively(dst, followLinks = false)
        }

        // the same behavior as link.copyTo(dst)
        dst.deleteExisting()
        assertFailsWith<java.nio.file.NoSuchFileException> {
            link.copyToRecursively(dst, followLinks = true)
        }
        assertFalse(dst.exists(LinkOption.NOFOLLOW_LINKS))
    }

    @Test
    fun copyBrokenSymlink() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")
        val target = createTempDirectory().cleanupRecursively().resolve("target")
        src.resolve("8/link").tryCreateSymbolicLinkTo(target) ?: return
        val dstLink = dst.resolve("8/link")

        // the same behavior as link.copyTo(dst, LinkOption.NOFOLLOW_LINKS)
        src.copyToRecursively(dst, followLinks = false)
        assertTrue(dstLink.isSymbolicLink())
        assertTrue(dstLink.exists(LinkOption.NOFOLLOW_LINKS))
        assertFalse(dstLink.exists())

        // the same behavior as link.copyTo(dst)
        dst.deleteRecursively()
        assertFailsWith<java.nio.file.NoSuchFileException> {
            src.copyToRecursively(dst, followLinks = true)
        }
        assertFalse(dstLink.exists(LinkOption.NOFOLLOW_LINKS))
    }

    @Test
    fun copyBaseSymlinkPointingToFile() {
        val src = createTempFile().cleanup().also { it.writeText("hello") }
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        link.copyToRecursively(dst, followLinks = false)
        compareFiles(link, dst)

        dst.deleteExisting()

        link.copyToRecursively(dst, followLinks = true)
        compareFiles(src, dst)
    }

    @Test
    fun copyBaseSymlinkPointingToDirectory() {
        val src = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        link.copyToRecursively(dst, followLinks = false)
        compareFiles(link, dst)

        dst.deleteExisting()

        link.copyToRecursively(dst, followLinks = true)
        compareDirectories(src, dst)
    }

    @Test
    fun copySymlinkPointingToDirectory() {
        val symlinkTarget = createTestFiles().cleanupRecursively()
        val src = createTestFiles().cleanupRecursively().also { it.resolve("8/link").tryCreateSymbolicLinkTo(symlinkTarget) ?: return }
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst, followLinks = false)
        val srcContent = listOf("", "8/link") + referenceFilenames
        testVisitedFiles(srcContent, dst.walkIncludeDirectories(), dst)

        dst.deleteRecursively()

        src.copyToRecursively(dst, followLinks = true)
        val expectedDstContent = srcContent + referenceFilenames.map { "8/link/$it" }
        testVisitedFiles(expectedDstContent, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copyIgnoreExistingDirectoriesFollowLinks() {
        val src = createTestFiles().cleanupRecursively()
        val symlinkTarget = createTempDirectory().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().also {
            it.resolve("1").createDirectory()
            it.resolve("1/3").tryCreateSymbolicLinkTo(symlinkTarget) ?: return
        }

        src.copyToRecursively(dst, followLinks = true, onError = { source, target, exception ->
            assertIs<java.nio.file.FileAlreadyExistsException>(exception)
            assertEquals(src.resolve("1/3"), source)
            assertEquals(dst.resolve("1/3"), target)
            assertEquals(target.toString(), exception.file)
            OnErrorResult.SKIP_SUBTREE
        })
        assertTrue(dst.resolve("1/3").isSymbolicLink())
        assertTrue(symlinkTarget.listDirectoryEntries().isEmpty())

        src.copyToRecursively(dst, followLinks = true, overwrite = true)
        assertFalse(dst.resolve("1/3").isSymbolicLink())
        assertTrue(symlinkTarget.listDirectoryEntries().isEmpty())
    }

    @Test
    fun copyIgnoreExistingDirectoriesNoFollowLinks() {
        val src = createTestFiles().cleanupRecursively()
        val symlinkTarget = createTempDirectory().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().also {
            it.resolve("1").createDirectory()
            it.resolve("1/3").tryCreateSymbolicLinkTo(symlinkTarget) ?: return
        }

        src.copyToRecursively(dst, followLinks = false, onError = { source, target, exception ->
            assertIs<java.nio.file.FileAlreadyExistsException>(exception)
            assertEquals(src.resolve("1/3"), source)
            assertEquals(dst.resolve("1/3"), target)
            assertEquals(target.toString(), exception.file)
            OnErrorResult.SKIP_SUBTREE
        })
        assertTrue(dst.resolve("1/3").isSymbolicLink())
        assertTrue(symlinkTarget.listDirectoryEntries().isEmpty())

        src.copyToRecursively(dst, followLinks = false, overwrite = true)
        assertFalse(dst.resolve("1/3").isSymbolicLink())
        assertTrue(symlinkTarget.listDirectoryEntries().isEmpty())
    }

    @Test
    fun copyParentSymlink() {
        val source = createTestFiles().cleanupRecursively()
        val linkToSource = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(source) ?: return
        val sources = listOf(
            source to referenceFilenames,
            linkToSource.resolve("8") to listOf("9.txt"),
            linkToSource.resolve("1/3") to listOf("4.txt", "5.txt")
        )

        for ((src, srcContent) in sources) {
            for (followLinks in listOf(false, true)) {
                val target = createTempDirectory().cleanupRecursively().also { it.resolve("a/b").createDirectories() }
                val linkToTarget = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(target) ?: return
                val targets = listOf(
                    target to listOf("a", "a/b"),
                    linkToTarget.resolve("a") to listOf("b"),
                    linkToTarget.resolve("a/b") to listOf()
                )

                for ((dst, dstContent) in targets) {
                    src.copyToRecursively(dst, followLinks = followLinks)
                    val expectedDstContent = listOf("") + dstContent + srcContent
                    testVisitedFiles(expectedDstContent, dst.walkIncludeDirectories(), dst)
                }
            }
        }
    }

    @Test
    fun copySymlinkToSymlink() {
        val src = createTestFiles().cleanupRecursively()
        val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return
        val linkToLink = createTempDirectory().cleanupRecursively().resolve("linkToLink").tryCreateSymbolicLinkTo(link) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        linkToLink.copyToRecursively(dst, followLinks = true)
        testVisitedFiles(listOf("") + referenceFilenames, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copySymlinkCyclic() {
        val src = createTestFiles().cleanupRecursively()
        val original = src.resolve("1")
        original.resolve("2/link").tryCreateSymbolicLinkTo(original) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst, followLinks = true, onError = { source, _, exception ->
            assertIs<java.nio.file.FileSystemLoopException>(exception)
            assertEquals(src.resolve("1/2/link"), source)
            assertEquals(source.toString(), exception.file)
            OnErrorResult.SKIP_SUBTREE
        })

        // partial copy, only "1/2/link" is not copied
        testVisitedFiles(listOf("") + referenceFilenames, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copySymlinkCyclicWithTwo() {
        val src = createTestFiles().cleanupRecursively()
        val dir8 = src.resolve("8")
        val dir2 = src.resolve("1/2")
        dir8.resolve("linkTo2").tryCreateSymbolicLinkTo(dir2) ?: return
        dir2.resolve("linkTo8").tryCreateSymbolicLinkTo(dir8) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        val loops = mutableListOf<String>()
        src.copyToRecursively(dst, followLinks = true, onError = { source, _, exception ->
            assertIs<java.nio.file.FileSystemLoopException>(exception)
            assertEquals(source.toString(), exception.file)
            loops.add(source.relativePathString(src))
            OnErrorResult.SKIP_SUBTREE
        })
        assertEquals(listOf("1/2/linkTo8/linkTo2", "8/linkTo2/linkTo8"), loops.sorted())

        // partial copy, only "1/2/linkTo8/linkTo2" and "8/linkTo2/linkTo8" are not copied
        val expected = listOf("", "1/2/linkTo8", "1/2/linkTo8/9.txt", "8/linkTo2") + referenceFilenames
        testVisitedFiles(expected, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copySymlinkPointingToItself() {
        val src = createTempDirectory().cleanupRecursively()
        val link = src.resolve("link")
        link.tryCreateSymbolicLinkTo(link) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        assertFailsWith<java.nio.file.FileSystemException> {
            // throws with message "Too many levels of symbolic links"
            src.copyToRecursively(dst, followLinks = true)
        }
    }

    @Test
    fun copySymlinkTwoPointingToEachOther() {
        val src = createTempDirectory().cleanupRecursively()
        val link1 = src.resolve("link1")
        val link2 = src.resolve("link2").tryCreateSymbolicLinkTo(link1) ?: return
        link1.tryCreateSymbolicLinkTo(link2) ?: return
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        assertFailsWith<java.nio.file.FileSystemException> {
            // throws with message "Too many levels of symbolic links"
            src.copyToRecursively(dst, followLinks = true)
        }
    }

    @Test
    fun copyWithNestedCopyToRecursively() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")
        val nested = createTestFiles().cleanupRecursively()

        src.copyToRecursively(dst, followLinks = false) { source, target ->
            if (source.name == "2") {
                nested.copyToRecursively(target, followLinks = false)
            } else {
                source.copyToIgnoringExistingDirectory(target, followLinks = false)
            }
            CopyActionResult.CONTINUE
        }

        val expected = listOf("") + referenceFilenames + referenceFilenames.map { "1/2/$it" }
        testVisitedFiles(expected, dst.walkIncludeDirectories(), dst)
    }

    @Test
    fun copyWithSkipSubtree() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst, followLinks = false) { source, target ->
            source.copyToIgnoringExistingDirectory(target, followLinks = false)
            if (source.name == "3" || source.name == "9.txt") {
                CopyActionResult.SKIP_SUBTREE
            } else {
                CopyActionResult.CONTINUE
            }
        }

        // both "3" and "9.txt" are copied
        val copied3 = dst.resolve("1/3").exists()
        val copied9 = dst.resolve("8/9.txt").exists()
        assertTrue(copied3 && copied9)

        // content of "3" is not copied
        assertTrue(dst.resolve("1/3").listDirectoryEntries().isEmpty())
    }

    @Test
    fun copyWithTerminate() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst, followLinks = false) { source, target ->
            source.copyToIgnoringExistingDirectory(target, followLinks = false)
            if (source.name == "3" || source.name == "9.txt") {
                CopyActionResult.TERMINATE
            } else {
                CopyActionResult.CONTINUE
            }
        }

        // either "3" or "9.txt" is not copied
        val copied3 = dst.resolve("1/3").exists()
        val copied9 = dst.resolve("8/9.txt").exists()
        assertTrue(copied3 || copied9)
        assertFalse(copied3 && copied9)
    }

    @Test
    fun copyFailureWithTerminate() {
        val src = createTestFiles().cleanupRecursively()
        val dst = createTempDirectory().cleanupRecursively().resolve("dst")

        src.copyToRecursively(dst, followLinks = false, onError = { source, _, exception ->
            assertIs<IllegalArgumentException>(exception)
            assertTrue(source.name == "3" || source.name == "9.txt")
            OnErrorResult.TERMINATE
        }) { source, target ->
            source.copyToIgnoringExistingDirectory(target, followLinks = false)
            if (source.name == "3" || source.name == "9.txt") throw IllegalArgumentException()
            CopyActionResult.CONTINUE
        }

        // either "3" or "9.txt" is not copied
        val copied3 = dst.resolve("1/3").exists()
        val copied9 = dst.resolve("8/9.txt").exists()
        assertTrue(copied3 || copied9)
        assertFalse(copied3 && copied9)
    }

    @Test
    fun copyIntoSourceDirectory() {
        val source = createTestFiles().cleanupRecursively()
        val linkToSource = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(source) ?: return
        val sources = listOf(
            source to source,
            linkToSource.resolve("8") to source.resolve("8"),
            linkToSource.resolve("1/3") to source.resolve("1/3")
        )

        for ((src, resolvedSrc) in sources) {
            val linkToSrc = createTempDirectory().cleanupRecursively().resolve("linkToSrc").tryCreateSymbolicLinkTo(resolvedSrc) ?: return
            val targets = listOf(
                linkToSrc.resolve("a").createDirectory(),
                linkToSrc.resolve("a/b").createDirectories()
            )

            for (followLinks in listOf(false, true)) {
                assertFailsWith<java.nio.file.FileAlreadyExistsException> {
                    src.copyToRecursively(linkToSrc, followLinks = followLinks)
                }
                for (dst in targets) {
                    val error = assertFailsWith<java.nio.file.FileSystemException> {
                        src.copyToRecursively(dst, followLinks = followLinks)
                    }
                    assertEquals("Recursively copying a directory into its subdirectory is prohibited.", error.reason)
                }
            }
        }
    }

    @Test
    fun kt38678() {
        val src = createTempDirectory().cleanupRecursively()
        src.resolve("test.txt").writeText("plain text file")

        val dst = src.resolve("x")

        val error = assertFailsWith<java.nio.file.FileSystemException> {
            src.copyToRecursively(dst, followLinks = false)
        }
        assertEquals("Recursively copying a directory into its subdirectory is prohibited.", error.reason)
    }

    @Test
    fun copyToTheSameFile() {
        for (src in listOf(createTempFile().cleanupRecursively(), createTestFiles().cleanupRecursively())) {
            src.copyToRecursively(src, followLinks = false)

            val link = createTempDirectory().cleanupRecursively().resolve("link").tryCreateSymbolicLinkTo(src) ?: return

            val error = assertFailsWith<java.nio.file.FileAlreadyExistsException> {
                link.copyToRecursively(src, followLinks = false)
            }
            assertEquals(src.toString(), error.file)
            link.copyToRecursively(src, followLinks = true)

            for (followLinks in listOf(false, true)) {
                assertFailsWith<java.nio.file.FileAlreadyExistsException> {
                    src.copyToRecursively(link, followLinks = followLinks)
                }
            }
        }
    }

    @Test
    fun copyDstLinkPointingToSrc() {
        for (followLinks in listOf(false, true)) {
            val root = createTempDirectory().cleanupRecursively()
            val src = root.resolve("src").createFile()
            val dstLink = root.resolve("dstLink").tryCreateSymbolicLinkTo(src) ?: return

            assertTrue(src.isSameFileAs(dstLink))
            assertTrue(dstLink.isSameFileAs(src))
            assertFailsWith<FileAlreadyExistsException> {
                src.copyToRecursively(dstLink, followLinks = followLinks)
            }
            assertTrue(dstLink.isSymbolicLink())
        }
    }

    @Test
    fun copyDstLinkPointingToSrcOverwrite() {
        for (followLinks in listOf(false, true)) {
            val root = createTempDirectory().cleanupRecursively()
            val src = root.resolve("src").createFile()
            val dstLink = root.resolve("dstLink").tryCreateSymbolicLinkTo(src) ?: return

            src.copyToRecursively(dstLink, followLinks = followLinks, overwrite = true)
            assertFalse(dstLink.isSymbolicLink())
        }
    }

    @Test
    fun copySrcLinkAndDstLinkPointingToSameFile() {
        for (followLinks in listOf(false, true)) {
            val root = createTempDirectory().cleanupRecursively()
            val original = root.resolve("original").createFile()
            val srcLink = root.resolve("srcLink").tryCreateSymbolicLinkTo(original) ?: return
            val dstLink = root.resolve("dstLink").tryCreateSymbolicLinkTo(original) ?: return

            assertTrue(srcLink.isSameFileAs(dstLink))
            assertTrue(dstLink.isSameFileAs(srcLink))
            assertFailsWith<FileAlreadyExistsException> {
                srcLink.copyToRecursively(dstLink, followLinks = followLinks)
            }
            assertTrue(dstLink.isSymbolicLink())
        }
    }

    @Test
    fun copySrcLinkAndDstLinkPointingToSameFileOverwrite() {
        for (followLinks in listOf(false, true)) {
            val root = createTempDirectory().cleanupRecursively()
            val original = root.resolve("original").createFile()
            val srcLink = root.resolve("srcLink").tryCreateSymbolicLinkTo(original) ?: return
            val dstLink = root.resolve("dstLink").tryCreateSymbolicLinkTo(original) ?: return

            srcLink.copyToRecursively(dstLink, followLinks = followLinks, overwrite = true)

            if (!followLinks) {
                assertTrue(dstLink.isSymbolicLink()) // src symlink was copied
            } else {
                assertFalse(dstLink.isSymbolicLink()) // target of src symlink was copied
            }
        }
    }

    @Test
    fun copySameLinkDifferentRoute() {
        for (followLinks in listOf(false, true)) {
            val root = createTempDirectory().cleanupRecursively()
            val original = root.resolve("original").createFile()
            val srcLink = root.resolve("srcLink").tryCreateSymbolicLinkTo(original) ?: return
            val dstLink = root.resolve("dstLink").tryCreateSymbolicLinkTo(root)?.resolve("srcLink") ?: return

            assertTrue(srcLink.isSameFileAs(dstLink))
            assertTrue(dstLink.isSameFileAs(srcLink))

            if (!followLinks) {
                srcLink.copyToRecursively(dstLink, followLinks = followLinks) // same file
            } else {
                assertFailsWith<FileAlreadyExistsException> {
                    srcLink.copyToRecursively(dstLink, followLinks = followLinks) // target of srcLink copied to srcLink location
                }
            }

            assertTrue(dstLink.isSymbolicLink())
        }
    }

    @Test
    fun copySameLinkDifferentRouteOverwrite() {
        for (followLinks in listOf(false, true)) {
            val root = createTempDirectory().cleanupRecursively()
            val original = root.resolve("original").createFile()
            val srcLink = root.resolve("srcLink").tryCreateSymbolicLinkTo(original) ?: return
            val dstLink = root.resolve("dstLink").tryCreateSymbolicLinkTo(root)?.resolve("srcLink") ?: return

            if (!followLinks) {
                srcLink.copyToRecursively(dstLink, followLinks = followLinks, overwrite = true) // same file
            } else {
                // dstLink is deleted before srcLink gets copied.
                // Actually srcLink gets removed because dstLink is srcLink with different path.
                val error = assertFailsWith<NoSuchFileException> {
                    srcLink.copyToRecursively(dstLink, followLinks = followLinks, overwrite = true)
                }
                assertEquals(srcLink.toString(), error.file)
                assertFalse(srcLink.exists(LinkOption.NOFOLLOW_LINKS))
                assertFalse(dstLink.exists(LinkOption.NOFOLLOW_LINKS))
            }
        }
    }

    @Test
    fun copySameFileDifferentRoute() {
        val root = createTempDirectory().cleanupRecursively()
        val src = root.resolve("src").createFile()
        val dst = root.resolve("dstLink").tryCreateSymbolicLinkTo(root)?.resolve("src") ?: return

        assertTrue(src.isSameFileAs(dst))
        assertTrue(dst.isSameFileAs(src))
        src.copyToRecursively(dst, followLinks = false)
    }

    @Test
    fun copyToSameFileDifferentRouteOverwrite() {
        val root = createTempDirectory().cleanupRecursively()
        val src = root.resolve("src").createFile()
        val dst = root.resolve("dstLink").tryCreateSymbolicLinkTo(root)?.resolve("src") ?: return

        src.copyToRecursively(dst, followLinks = false, overwrite = true)
    }

    private fun createZipFile(parent: Path, name: String, entries: List<String>): Path {
        val zipRoot = parent.resolve(name)
        ZipOutputStream(zipRoot.outputStream()).use { out ->
            for (fileName in entries) {
                out.putNextEntry(ZipEntry(fileName))
                if (!fileName.endsWith("/")) {
                    out.write(fileName.toByteArray())
                }
                out.closeEntry()
            }
        }
        return zipRoot
    }

    @Test
    fun zipToDefaultPath() {
        val root = createTempDirectory().cleanupRecursively()
        val zipRoot = createZipFile(root, "src.zip", listOf("directory/", "directory/file.txt"))
        val dst = root.resolve("dst")

        val classLoader: ClassLoader? = null
        FileSystems.newFileSystem(zipRoot, classLoader).use { zipFs ->
            val src = zipFs.getPath("/directory")

            src.copyToRecursively(dst, followLinks = false)

            val expected = listOf("", "file.txt")
            testVisitedFiles(expected, dst.walkIncludeDirectories(), dst)
            assertEquals("directory/file.txt", dst.resolve("file.txt").readText())
        }
    }

    @Test
    fun defaultPathToZip() {
        val root = createTestFiles().cleanupRecursively()
        val zipRoot = createZipFile(root, "dst.zip", listOf("directory/", "directory/file.txt"))
        val src = root.resolve("1").also { it.resolve("3/4.txt").writeText("hello") }

        val classLoader: ClassLoader? = null
        FileSystems.newFileSystem(zipRoot, classLoader).use { zipFs ->
            val dst = zipFs.getPath("/directory")

            src.copyToRecursively(dst, followLinks = false)

            val expected = listOf("", "2", "3", "3/4.txt", "3/5.txt", "file.txt")
            testVisitedFiles(expected, dst.walkIncludeDirectories(), dst)
            assertEquals("hello", zipFs.getPath("/directory/3/4.txt").readText())
        }
    }

    private fun withZip(name: String, entries: List<String>, block: (parent: Path, zipRoot: Path) -> Unit) {
        val parent = createTempDirectory().cleanupRecursively()
        val archive = createZipFile(parent, name, entries)
        val zipFs: FileSystem
        try {
            val classLoader: ClassLoader? = null
            zipFs = FileSystems.newFileSystem(archive, classLoader)
        } catch (e: ZipException) {
            // Later JDK versions do not allow opening zip files that have entry named "." or "..".
            // See https://bugs.openjdk.org/browse/JDK-8251329 and https://bugs.openjdk.org/browse/JDK-8283486
            println("Opening a ZIP file failed with $e")
            return
        }
        zipFs.use {
            val zipRoot = it.getPath("/")
            block(parent, zipRoot)
        }
    }

    private fun testWalkSucceeds(path: Path, vararg expectedContent: Set<Path>) {
        val content = path.walkIncludeDirectories().toSet()
        assertContains(expectedContent.toList(), content)
    }

    private fun testWalkFailsWithIllegalFileName(path: Path) {
        assertFailsWith<IllegalFileNameException> {
            path.walkIncludeDirectories().toList()
        }
    }

    private inline fun <reified T> testWalkMaybeFailsWith(path: Path, vararg expectedContent: Set<Path>) {
        try {
            testWalkSucceeds(path, *expectedContent)
        } catch (exception: Exception) {
            assertIs<T>(exception)
        }
    }

    private fun testCopySucceeds(source: Path, target: Path, vararg expectedTargetContent: Set<Path>) {
        source.copyToRecursively(target, followLinks = false)
        val content = target.walkIncludeDirectories().toSet()
        assertContains(expectedTargetContent.toList(), content)
    }

    private fun testCopyFailsWithIllegalFileName(source: Path, target: Path) {
        assertFailsWith<IllegalFileNameException> {
            source.copyToRecursively(target, followLinks = false)
        }
    }

    private inline fun <reified T> testCopyMaybeFailsWith(source: Path, target: Path, expectedTargetContent: Set<Path>) {
        try {
            testCopySucceeds(source, target, expectedTargetContent)
        } catch (exception: Exception) {
            assertIs<T>(exception)
        }
    }

    private fun testDeleteSucceeds(path: Path) {
        path.deleteRecursively()
        assertFalse(path.exists())
    }

    private inline fun <reified T> testDeleteFailsWith(path: Path) {
        assertFailsWith<FileSystemException> {
            path.deleteRecursively()
        }.also { exception ->
            val suppressed = exception.suppressed.single()
            assertIs<T>(suppressed)
        }
    }

    private inline fun <reified T> testDeleteMaybeFailsWith(path: Path) {
        try {
            testDeleteSucceeds(path)
        } catch (exception: FileSystemException) {
            val suppressed = exception.suppressed.single()
            assertIs<T>(suppressed)
        }
    }

    private fun Path.resolve(vararg entryNames: String): Set<Path> {
        return entryNames.map { resolve(it) }.toSet()
    }

    @Test
    fun zipDotFileName() {
        withZip("Archive1.zip", listOf("normal", ".")) { root, zipRoot ->
            val dotFile = zipRoot.resolve(".")
            val dotDir = zipRoot.resolve("./")
            testWalkFailsWithIllegalFileName(zipRoot)
            testWalkFailsWithIllegalFileName(dotFile)
            // Succeeds on jvm8, fails on jvm9+
            testWalkMaybeFailsWith<IllegalFileNameException>(dotDir, setOf(dotDir))

            val target = root.resolve("UnzipArchive1")
            testCopyFailsWithIllegalFileName(zipRoot, target)
            val dotFileTarget = root.resolve("UnzipArchive1-dotFile")
            testCopyFailsWithIllegalFileName(dotFile, dotFileTarget)
            val dotDirTarget = root.resolve("UnzipArchive1-dotDir")
            // Succeeds on jvm8, fails on jvm9+
            testCopyMaybeFailsWith<IllegalFileNameException>(dotDir, dotDirTarget, setOf(dotDirTarget))

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
            testDeleteFailsWith<IllegalFileNameException>(dotFile)
            // Succeeds on jvm8, fails on jvm9+
            testDeleteMaybeFailsWith<IllegalFileNameException>(dotDir)
            assertTrue(dotFile.exists())
            assertTrue(zipRoot.exists())
        }

        withZip("Archive2.zip", listOf("normal", "./")) { root, zipRoot ->
            val dotFile = zipRoot.resolve(".")
            val dotDir = zipRoot.resolve("./")
            testWalkFailsWithIllegalFileName(zipRoot)
            testWalkFailsWithIllegalFileName(dotFile)
            // Succeeds on jvm8, fails on jvm9+
            testWalkMaybeFailsWith<IllegalFileNameException>(dotDir, setOf(dotDir))

            val target = root.resolve("UnzipArchive2")
            testCopyFailsWithIllegalFileName(zipRoot, target)
            val dotFileTarget = root.resolve("UnzipArchive2-dotFile")
            testCopyFailsWithIllegalFileName(dotFile, dotFileTarget)
            val dotDirTarget = root.resolve("UnzipArchive2-dotDir")
            // Succeeds on jvm8, fails on jvm9+
            testCopyMaybeFailsWith<IllegalFileNameException>(dotDir, dotDirTarget, setOf(dotDirTarget))

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
            testDeleteFailsWith<IllegalFileNameException>(dotFile)
            // Succeeds on jvm8, fails on jvm9+
            testDeleteMaybeFailsWith<IllegalFileNameException>(dotDir)
            assertTrue(dotFile.exists())
            assertTrue(zipRoot.exists())
        }

        withZip("Archive3.zip", listOf("a/", "a/.")) { root, zipRoot ->
            val a = zipRoot.resolve("a")
            testWalkFailsWithIllegalFileName(zipRoot)
            testWalkFailsWithIllegalFileName(a)

            val target = root.resolve("UnzipArchive3")
            testCopyFailsWithIllegalFileName(zipRoot, target)
            val aTarget = root.resolve("UnzipArchive3-a")
            testCopyFailsWithIllegalFileName(a, aTarget)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
            testDeleteFailsWith<IllegalFileNameException>(a)
        }

        withZip("Archive4.zip", listOf("a/", "a/./")) { root, zipRoot ->
            val a = zipRoot.resolve("a")
            testWalkFailsWithIllegalFileName(zipRoot)
            testWalkFailsWithIllegalFileName(a)

            val target = root.resolve("UnzipArchive4")
            testCopyFailsWithIllegalFileName(zipRoot, target)
            val aTarget = root.resolve("UnzipArchive4-a")
            testCopyFailsWithIllegalFileName(a, aTarget)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
            testDeleteFailsWith<IllegalFileNameException>(a)
        }
    }

    @Test
    fun zipSlashFileName() {
        withZip("Archive1.zip", listOf("normal", "/")) { root, zipRoot ->
            // Fails in jvm8-10, succeeds in jvm11
            testWalkMaybeFailsWith<FileSystemLoopException>(zipRoot, zipRoot.resolve("", "normal"))

            val target = root.resolve("UnzipArchive1")
            // Fails in jvm8-10, succeeds in jvm11
            testCopyMaybeFailsWith<FileSystemLoopException>(zipRoot, target, target.resolve("", "normal"))

            // Throws FileSystemLoopException in jvm8-10, Path.deleteIfExists throws NullPointerException in jvm11
            assertFails { zipRoot.deleteRecursively() }
        }

        withZip("Archive2.zip", listOf("normal", "//")) { root, zipRoot ->
            // Fails in jvm8, succeeds in jvm9+
            try {
                zipRoot.walkIncludeDirectories().toList()
                // ["/", "//", "normal"] in jvm9-10
                // ["/", "/normal"] in jvm11
            } catch (exception: Exception) {
                assertIs<FileSystemLoopException>(exception)
            }

            val target = root.resolve("UnzipArchive2")
            // Fails in jvm8, succeeds in jvm9+
            testCopyMaybeFailsWith<FileSystemLoopException>(zipRoot, target, target.resolve("", "normal"))

            // Throws FileSystemLoopException in jvm8, Path.deleteIfExists throws NullPointerException in jvm9+
            assertFails { zipRoot.deleteRecursively() }
        }

        withZip("Archive3.zip", listOf("a/", "a//")) { root, zipRoot ->
            val aFile = zipRoot.resolve("a")
            val aDir = zipRoot.resolve("a/")
            // Fails in jvm8, succeeds in jvm9+
            try {
                zipRoot.walkIncludeDirectories().toList()
                // ["/", "/a", "/a/"] in jvm9-10
                // ["/", "/a"] in jvm11
            } catch (exception: Exception) {
                assertIs<FileSystemLoopException>(exception)
            }
            testWalkMaybeFailsWith<FileSystemLoopException>(aFile, setOf(aFile))
            testWalkMaybeFailsWith<FileSystemLoopException>(aDir, setOf(aFile))

            // Fails in jvm8, succeeds in jvm9+
            val zipRootTarget = root.resolve("UnzipArchive3")
            testCopyMaybeFailsWith<FileSystemLoopException>(zipRoot, zipRootTarget, zipRootTarget.resolve("", "a"))
            val aFileTarget = root.resolve("UnzipArchive3-aFile")
            testCopyMaybeFailsWith<FileSystemLoopException>(aFile, aFileTarget, setOf(aFileTarget))
            val aDirTarget = root.resolve("UnzipArchive3-aDir")
            testCopyMaybeFailsWith<FileSystemLoopException>(aDir, aDirTarget, setOf(aDirTarget))

            // Throws FileSystemLoopException in jvm8, Path.deleteIfExists throws NullPointerException in jvm9+
            assertFails { zipRoot.deleteRecursively() }
            // Fails in jvm8, succeeds in jvm9+
            testDeleteMaybeFailsWith<FileSystemLoopException>(aFile)
            testDeleteMaybeFailsWith<FileSystemLoopException>(aDir)
        }
    }

    // KT-63103
    @Test
    fun zipDoubleDotsFileName() {
        withZip("Archive1.zip", listOf("normal", "../sneaky")) { root, zipRoot ->
            root.resolve("sneaky").createFile().also { it.writeText("outer sneaky") }
            testWalkFailsWithIllegalFileName(zipRoot)

            val target = root.resolve("UnzipArchive1")
            testCopyFailsWithIllegalFileName(zipRoot, target)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
        }
        withZip("Archive2.zip", listOf("normal", "../normal")) { root, zipRoot ->
            testWalkFailsWithIllegalFileName(zipRoot)

            val target = root.resolve("UnzipArchive2")
            testCopyFailsWithIllegalFileName(zipRoot, target)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
        }

        withZip("Archive3.zip", listOf("normal", "../")) { root, zipRoot ->
            testWalkFailsWithIllegalFileName(zipRoot)

            val target = root.resolve("UnzipArchive3")
            testCopyFailsWithIllegalFileName(zipRoot, target)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
        }

        withZip("Archive4.zip", listOf("normal", "..")) { root, zipRoot ->
            testWalkFailsWithIllegalFileName(zipRoot)

            val target = root.resolve("UnzipArchive4")
            testCopyFailsWithIllegalFileName(zipRoot, target)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
        }

        withZip("Archive5.zip", listOf("normal", "../..")) { root, zipRoot ->
            testWalkFailsWithIllegalFileName(zipRoot)

            val target = root.resolve("UnzipArchive5")
            testCopyFailsWithIllegalFileName(zipRoot, target)

            testDeleteFailsWith<IllegalFileNameException>(zipRoot)
        }

        withZip("Archive6.zip", listOf("normal", "../")) { root, zipRoot ->
            val targetParent = root.resolve("UnzipArchive6Parent").createDirectory()
            val targetSibling = targetParent.resolve("UnzipArchive6Sibling").createFile()
            val target = targetParent.resolve("UnzipArchive6").createDirectory()
            assertFailsWith<IllegalFileNameException> {
                zipRoot.copyToRecursively(target, followLinks = false) { src, dst ->
                    if (dst != target) {
                        dst.deleteRecursively()
                        src.copyTo(dst)
                    }
                    CopyActionResult.CONTINUE
                }
            }

            assertTrue(target.exists())
            assertTrue(targetSibling.exists())
        }

        withZip("Archive7.zip", listOf("normal", "..a..b..")) { root, zipRoot ->
            testWalkSucceeds(zipRoot, zipRoot.resolve("", "normal", "..a..b.."))

            val target = root.resolve("UnzipArchive7")
            val unix = target.resolve("", "normal", "..a..b..") // In Linux and macOS
            val windows = target.resolve("", "normal", "..a..b") // In Windows
            testCopySucceeds(zipRoot, target, unix, windows)

            // Path.deleteIfExists throws NullPointerException
            testDeleteFailsWith<NullPointerException>(zipRoot)
            assertEquals(emptyList(), zipRoot.listDirectoryEntries())
        }

        withZip("Archive8.zip", listOf("b", "a/", "a/../b")) { root, zipRoot ->
            val a = zipRoot.resolve("a")

            testWalkFailsWithIllegalFileName(a)

            val target = root.resolve("UnzipArchive8")
            testCopyFailsWithIllegalFileName(a, target)

            testDeleteFailsWith<IllegalFileNameException>(a)
        }

        withZip("Archive9.zip", listOf("b/", "b/d", "a/", "a/../b/c")) { root, zipRoot ->
            val b = zipRoot.resolve("a/../b")
            // Traverses the "b" directory outside "a"
            val jvm8 = zipRoot.resolve("a/../b", "b/d")
            val jvm11 = zipRoot.resolve("a/../b", "a/../b/d")
            testWalkSucceeds(b, jvm8, jvm11)

            val target = root.resolve("UnzipArchive9")
            // Copied content of the "b" directory that is outside "a"
            testCopySucceeds(b, target, target.resolve("", "d"))

            testDeleteSucceeds(b)
            // The deleted "/a/../b" path actually deleted the "b" outside "a"
            assertNull(zipRoot.listDirectoryEntries().find { it.name == "b" })
        }
    }

    @Test
    fun copyIllegalFileNameExceptionPassedToOnError() {
        withZip("Archive1.zip", listOf("normal", "..")) { root, zipRoot ->
            val target = root.resolve("UnzipArchive1")
            var failed = false
            zipRoot.copyToRecursively(target, followLinks = false, onError = { _, _, exception ->
                failed = true
                assertIs<IllegalFileNameException>(exception)
                OnErrorResult.SKIP_SUBTREE
            })
            assertTrue(failed)
        }
        withZip("Archive2.zip", listOf("normal", "/")) { root, zipRoot ->
            val target = root.resolve("UnzipArchive2")
            // FileSystemLoopException in jvm8-10, no exception in jvm11
            zipRoot.copyToRecursively(target, followLinks = false, onError = { _, _, exception ->
                assertIs<FileSystemLoopException>(exception)
                OnErrorResult.SKIP_SUBTREE
            })
        }
        withZip("Archive3.zip", listOf("normal", ".")) { root, zipRoot ->
            val target = root.resolve("UnzipArchive3")
            var failed = false
            zipRoot.copyToRecursively(target, followLinks = false, onError = { _, _, exception ->
                failed = true
                assertIs<IllegalFileNameException>(exception)
                OnErrorResult.SKIP_SUBTREE
            })
            assertTrue(failed)
        }
    }

    @Test
    // TODO: Should recursive functions normalize the Path at the beginning ?
    fun legalDirectorySymbols() {
        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/.")

            testWalkSucceeds(path, path.resolve("", "4.txt", "5.txt"))

            val target = root.resolve("target")
            testCopySucceeds(path, target, target.resolve("", "4.txt", "5.txt"))

            // Fails in Linux and macOS, succeeds in Windows
            // deleteIfExists/deleteDirectory() throws FileSystemException: /1/3/.: Invalid argument
            testDeleteMaybeFailsWith<FileSystemException>(path)
        }

        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/4.txt/.")

            val unix = emptySet<Path>() // In Linux and macOS
            val windows = setOf(path) // In Windows
            testWalkSucceeds(path, unix, windows)

            val target = root.resolve("target")
            // Copy fails in Linux and macOS, succeeds in Windows
            // Path.copyToRecursively throws NoSuchFileException: /1/3/4.txt/.: The source file doesn't exist.
            testCopyMaybeFailsWith<NoSuchFileException>(path, target, setOf(target))

            // Delete fails in Linux and macOS, succeeds in Windows
            // Path.deleteIfExists() throws FileSystemException: /1/3/4.txt/.: Not a directory
            testDeleteMaybeFailsWith<FileSystemException>(path)
        }

        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/..")

            testWalkSucceeds(path, path.resolve("", "2", "3", "3/4.txt", "3/5.txt"))

            val target = root.resolve("target")
            testCopySucceeds(path, target, target.resolve("", "2", "3", "3/4.txt", "3/5.txt"))

            // Fails in macOS and Linux, succeeds in Windows
            // In macOS Path.isSameFileAs() throws NoSuchFileException: /1/3/../2
            // In Linux deleteDirectory() throws DirectoryNotEmptyException wrapped into FileSystemException: /1/3/..
            testDeleteMaybeFailsWith<FileSystemException>(path)
        }

        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/4.txt/..")

            val unix = emptySet<Path>() // In Linux and macOS
            val windows = path.resolve("", "4.txt", "5.txt") // In Windows
            testWalkSucceeds(path, unix, windows)

            val target = root.resolve("target")
            // Copy fails in Linux and macOS, succeeds in Windows
            // Path.copyToRecursively throws NoSuchFileException: /1/3/4.txt/..: The source file doesn't exist.
            testCopyMaybeFailsWith<NoSuchFileException>(path, target, setOf(target, target.resolve("4.txt"), target.resolve("5.txt")))

            // Delete fails in Linux and macOS, succeeds in Windows
            // Path.deleteIfExists() throws FileSystemException: /1/3/4.txt/..: Not a directory
            testDeleteMaybeFailsWith<FileSystemException>(path)
        }
    }
}
