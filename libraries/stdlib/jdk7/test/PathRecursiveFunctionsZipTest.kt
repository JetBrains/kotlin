/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import test.testOnJvm8
import test.testOnJvm9AndAbove
import java.lang.NullPointerException
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.jdk7.test.PathTreeWalkTest.Companion.createTestFiles
import kotlin.jdk7.test.PathTreeWalkTest.Companion.referenceFilenames
import kotlin.jdk7.test.PathTreeWalkTest.Companion.testVisitedFiles
import kotlin.test.*

class PathRecursiveFunctionsZipTest : AbstractPathTest() {

    private fun Path.walkIncludeDirectories(): Sequence<Path> =
        this.walk(PathWalkOption.INCLUDE_DIRECTORIES)

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
            if (e.message?.contains("a '.' or '..' element") == true) {
                println("Opening a ZIP file failed with $e")
            } else {
                // Potentially a different cause for the failed opening.
                throw e
            }
            return
        }
        zipFs.use {
            val zipRoot = it.getPath("/")
            block(parent, zipRoot)
        }
    }


    @Test
    fun zipToDefaultPath() {
        withZip("src.zip", listOf("directory/", "directory/file.txt")) { root, zipRoot ->
            val dst = root.resolve("dst")
            val src = zipRoot.resolve("directory")

            src.copyToRecursively(dst, followLinks = false)

            val expected = listOf("", "file.txt")
            testVisitedFiles(expected, dst.walkIncludeDirectories(), dst)
            assertEquals("directory/file.txt", dst.resolve("file.txt").readText())
        }
    }

    @Test
    fun defaultPathToZip() {
        val srcRoot = createTestFiles().cleanupRecursively()
        withZip("dst.zip", listOf("directory/", "directory/file.txt")) { _, zipRoot ->
            val src = srcRoot.resolve("1").also { it.resolve("3/4.txt").writeText("hello") }
            val dst = zipRoot.resolve("directory")

            src.copyToRecursively(dst, followLinks = false)

            val expected = listOf("", "2", "3", "3/4.txt", "3/5.txt", "file.txt")
            testVisitedFiles(expected, dst.walkIncludeDirectories(), dst)
            assertEquals("hello", zipRoot.resolve("directory/3/4.txt").readText())
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

    private inline fun <reified T> testDeleteMaybeFailsWith(path: Path): Boolean {
        return try {
            testDeleteSucceeds(path)
            true
        } catch (exception: FileSystemException) {
            val suppressed = exception.suppressed.single()
            assertIs<T>(suppressed)
            false
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

            // Throws FileSystemLoopException in jvm8-10
            // Path.deleteIfExists on the root directory of the archive throws NullPointerException in jvm9+
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

            // Throws FileSystemLoopException in jvm8
            // Path.deleteIfExists on the root directory of the archive throws NullPointerException in jvm9+
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
            // Fails with jdk8 "IllegalFileNameException: Copying files to outside the specified target directory is prohibited."
            testCopyMaybeFailsWith<IllegalFileNameException>(aDir, aDirTarget, setOf(aDirTarget))

            // Throws FileSystemLoopException in jvm8
            // Path.deleteIfExists on the root directory of the archive throws NullPointerException in jvm9+
            assertFails { zipRoot.deleteRecursively() }
            // Fails in jvm8, succeeds in jvm9+
            testDeleteMaybeFailsWith<FileSystemLoopException>(aFile)
            testDeleteMaybeFailsWith<FileSystemLoopException>(aDir)
        }
    }

    @Test
    fun copyOutsideTargetIsProhibited() {
        withZip("Archive.zip", listOf("a", "a//")) { root, zipRoot ->
            val aDir = zipRoot.resolve("a/")
            testWalkSucceeds(aDir, zipRoot.resolve("a/", "a"))

            val aDirTarget = root.resolve("UnzipArchive-aDir")
            // Fails with jdk8 "IllegalFileNameException: Copying files to outside the specified target directory is prohibited."
            testCopyMaybeFailsWith<IllegalFileNameException>(aDir, aDirTarget, setOf(aDirTarget))
            // No file is copied outside the target
            testWalkSucceeds(root, root.resolve("", "Archive.zip", "UnzipArchive-aDir"))
        }
    }

    @Test
    fun deleteZipRootDirectory() {
        withZip("Archive.zip", emptyList()) { _, zipRoot ->
            // Deleting the root directory of a zip archive throws NullPointerException.
            assertFailsWith<NullPointerException> {
                zipRoot.deleteIfExists()
            }
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

            // Path.deleteIfExists on the root directory of the archive throws NullPointerException
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
    fun zipWindowsPathSeparators() {
        // When creating a zip archive, entries are added with the exact given names.
        testOnJvm8 {
            // JDK8 converts backslashes to slashes when reading entries, but later can't find those entries
            withZip("Archive1.zip", listOf("b\\", "b\\d", "a\\")) { root, zipRoot ->
                assertFailsWith<NoSuchFileException> {
                    zipRoot.walkIncludeDirectories().toList()
                }

                // There is no directory with name "a", thus empty walk sequence
                testWalkSucceeds(zipRoot.resolve("a"), emptySet())

                assertFailsWith<NoSuchFileException> {
                    val target = root.resolve("UnzipArchive1")
                    zipRoot.copyToRecursively(target, followLinks = false)
                }

                assertFailsWith<FileSystemException> {
                    zipRoot.deleteRecursively()
                }.also { exception ->
                    exception.suppressed.forEach {
                        assertIs<NoSuchFileException>(it)
                    }
                }
            }
        }

        testOnJvm9AndAbove {
            // JDK9+ treats backslashes as part of entry name
            withZip("Archive1.zip", listOf("b\\", "b\\d", "a\\", "a\\..\\b\\c")) { root, zipRoot ->
                val expectedWalk = listOf("", "b\\", "b\\d", "a\\", "a\\..\\b\\c").map { "/$it" }.toSet()
                val walk = zipRoot.walkIncludeDirectories().map { it.toString() }.toSet()
                assertEquals(expectedWalk, walk)

                // There is no directory with name "a", thus empty walk sequence
                testWalkSucceeds(zipRoot.resolve("a"), emptySet())

                val target = root.resolve("UnzipArchive1")
                // Fails in Windows
                testCopyMaybeFailsWith<NoSuchFileException>(zipRoot, target, target.resolve("", "b\\", "b\\d", "a\\", "a\\..\\b\\c"))

                // Deleting a zip root throws NPE
                testDeleteFailsWith<NullPointerException>(zipRoot)
                // All entries inside are deleted
                testWalkSucceeds(zipRoot, setOf(zipRoot))
            }
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

    // To demonstrate how recursive functions behave when the traversal starting path ends with "." or ".."
    // in the default file system, not inside a zip.
    @Test
    fun legalDirectorySymbols() {
        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/.")

            testWalkSucceeds(path, path.resolve("", "4.txt", "5.txt"))

            val target = createTempDirectory().cleanupRecursively().resolve("target")
            testCopySucceeds(path, target, target.resolve("", "4.txt", "5.txt"))

            // Fails in Linux and macOS, succeeds in Windows
            // deleteIfExists/deleteDirectory() throws FileSystemException: /1/3/.: Invalid argument
            val deleteSucceeded = testDeleteMaybeFailsWith<FileSystemException>(path)
            if (deleteSucceeded) {
                // Only the "1/3" directory and its content is deleted
                val expectedRootContent = setOf(root) + referenceFilenames.filter { !it.contains("3") }.map { root.resolve(it) }
                testWalkSucceeds(root, expectedRootContent)
            }
        }

        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/4.txt/.")

            val unix = emptySet<Path>() // In Linux and macOS
            val windows = setOf(path) // In Windows
            testWalkSucceeds(path, unix, windows)

            val target = createTempDirectory().cleanupRecursively().resolve("target")
            // Copy fails in Linux and macOS, succeeds in Windows
            // Path.copyToRecursively throws NoSuchFileException: /1/3/4.txt/.: The source file doesn't exist.
            testCopyMaybeFailsWith<NoSuchFileException>(path, target, setOf(target))

            // Delete fails in Linux and macOS, succeeds in Windows
            // Path.deleteIfExists() throws FileSystemException: /1/3/4.txt/.: Not a directory
            val deleteSucceeded = testDeleteMaybeFailsWith<FileSystemException>(path)
            if (deleteSucceeded) {
                // Only the "1/3/4.txt" file is deleted
                val expectedRootContent = setOf(root) + referenceFilenames.filter { !it.contains("4.txt") }.map { root.resolve(it) }
                testWalkSucceeds(root, expectedRootContent)
            }
        }

        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/..")

            testWalkSucceeds(path, path.resolve("", "2", "3", "3/4.txt", "3/5.txt"))

            val target = createTempDirectory().cleanupRecursively().resolve("target")
            testCopySucceeds(path, target, target.resolve("", "2", "3", "3/4.txt", "3/5.txt"))

            // Fails in macOS and Linux, succeeds in Windows
            // In macOS Path.isSameFileAs() throws NoSuchFileException: /1/3/../2
            // In Linux deleteDirectory() throws DirectoryNotEmptyException wrapped into FileSystemException: /1/3/..
            val deleteSucceeded = testDeleteMaybeFailsWith<FileSystemException>(path)
            if (deleteSucceeded) {
                // Only the "1" directory and its content is deleted
                val expectedRootContent = setOf(root) + referenceFilenames.filter { !it.contains("1") }.map { root.resolve(it) }
                testWalkSucceeds(root, expectedRootContent)
            }
        }

        createTestFiles().cleanupRecursively().let { root ->
            val path = root.resolve("1/3/4.txt/..")

            val unix = emptySet<Path>() // In Linux and macOS
            val windows = path.resolve("", "4.txt", "5.txt") // In Windows
            testWalkSucceeds(path, unix, windows)

            val target = createTempDirectory().cleanupRecursively().resolve("target")
            // Copy fails in Linux and macOS, succeeds in Windows
            // Path.copyToRecursively throws NoSuchFileException: /1/3/4.txt/..: The source file doesn't exist.
            testCopyMaybeFailsWith<NoSuchFileException>(path, target, setOf(target, target.resolve("4.txt"), target.resolve("5.txt")))

            // Delete fails in Linux and macOS, succeeds in Windows
            // Path.deleteIfExists() throws FileSystemException: /1/3/4.txt/..: Not a directory
            val deleteSucceeded = testDeleteMaybeFailsWith<FileSystemException>(path)
            if (deleteSucceeded) {
                // Only the "1/3" directory and its content is deleted
                val expectedRootContent = setOf(root) + referenceFilenames.filter { !it.contains("3") }.map { root.resolve(it) }
                testWalkSucceeds(root, expectedRootContent)
            }
        }
    }
}