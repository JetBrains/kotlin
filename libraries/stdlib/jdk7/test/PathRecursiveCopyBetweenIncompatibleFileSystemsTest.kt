/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.File
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PathRecursiveCopyBetweenIncompatibleFileSystemsTest : AbstractPathTest() {
    @Test // Regression test for KT-85020
    fun copyBetweenFilesystemsWithIncompatiblePathSeparators() {
        val root = createTempDirectory().cleanupRecursively()

        val srcDirOriginal = root.resolve("src").also {
            it.resolve("a")
                .resolve("b")
                .resolve("c")
                .resolve("test.txt")
                .createParentDirectories()
                .writeText("hello")
        }

        val dst = root.resolve("dst").createDirectory()

        val mockFs = SharpFileSystemProvider().newTestFileSystem(srcDirOriginal.fileSystem)
        val mockedSrc = mockFs.wrap(srcDirOriginal)

        mockedSrc.copyToRecursively(dst, followLinks = false, overwrite = false)

        val expectedPaths = srcDirOriginal.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .map { it.relativeTo(srcDirOriginal).toString() }
            .toSet()
        val actualPaths = dst.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .map { it.relativeTo(dst).toString() }
            .toSet()
        assertEquals(expectedPaths, actualPaths)
    }

    @Ignore // Names containing path separators are handled incorrectly now.
    @Test
    fun copyFileWithNameCorrespondingToMultipleSegments() {
        val root = createTempDirectory().cleanupRecursively()

        val src = root.resolve("src").also {
            it.resolve("a")
                .resolve("b#test.txt")
                .createParentDirectories()
                .writeText("hello")
        }

        val dst = root.resolve("dst").createDirectory()

        val mockFs = SharpFileSystemProvider().newTestFileSystem(dst.fileSystem)
        val mockedDst = mockFs.wrap(dst)

        assertFailsWith<FileSystemException> {
            src.copyToRecursively(mockedDst, followLinks = false, overwrite = false)
        }.also {
            assertContains(it.message!!, "b#test.txt")
        }
    }
}

// FileSystemProvide, FileSystem and Path implementations below exists for testing purposes only,
// and solve the only one purpose - wrap an existing file system so that all
// its path will use "#" as a path separator. Thus, the name - SharpSomething.
private class SharpFileSystemProvider : FileSystemProvider() {
    private var fileSystem: SharpFileSystem? = null

    fun newTestFileSystem(delegateFileSystem: FileSystem): SharpFileSystem {
        check(fileSystem == null) { "Only one test filesystem can be registered in the provider" }
        return SharpFileSystem(this, delegateFileSystem).also { fileSystem = it }
    }

    override fun getScheme(): String = "sharp"

    override fun newFileSystem(uri: URI, env: MutableMap<String, *>): FileSystem = TODO()

    override fun getFileSystem(uri: URI): FileSystem = requireRegisteredFileSystem()

    override fun getPath(uri: URI): Path = requireRegisteredFileSystem().getPath(uri.path ?: "")

    override fun newByteChannel(
        path: Path,
        options: MutableSet<out OpenOption>,
        vararg attrs: FileAttribute<*>,
    ): SeekableByteChannel = delegateProvider().newByteChannel(path.toDelegatePath(), options, *attrs)

    override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> {
        val testDir = dir as SharpPath
        val stream = delegateProvider().newDirectoryStream(testDir.delegatePath) { delegatePath ->
            filter.accept(testDir.fileSystem.wrap(delegatePath))
        }
        return object : DirectoryStream<Path> {
            override fun iterator(): MutableIterator<Path> = object : MutableIterator<Path> {
                private val iterator = stream.iterator()

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): Path = testDir.fileSystem.wrap(iterator.next())

                override fun remove() = iterator.remove()
            }

            override fun close() = stream.close()
        }
    }

    override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
        delegateProvider().createDirectory(dir.toDelegatePath(), *attrs)
    }

    override fun delete(path: Path) {
        delegateProvider().delete(path.toDelegatePath())
    }

    override fun copy(source: Path, target: Path, vararg options: CopyOption) {
        delegateProvider().copy(source.toDelegatePath(), target.toDelegatePath(), *options)
    }

    override fun move(source: Path, target: Path, vararg options: CopyOption) {
        delegateProvider().move(source.toDelegatePath(), target.toDelegatePath(), *options)
    }

    override fun isSameFile(path: Path, path2: Path): Boolean {
        if (path !is SharpPath || path2 !is SharpPath) return false
        return delegateProvider().isSameFile(path.toDelegatePath(), path2.toDelegatePath())
    }

    override fun isHidden(path: Path): Boolean = delegateProvider().isHidden(path.toDelegatePath())

    override fun getFileStore(path: Path): FileStore = delegateProvider().getFileStore(path.toDelegatePath())

    override fun checkAccess(path: Path, vararg modes: AccessMode) {
        delegateProvider().checkAccess(path.toDelegatePath(), *modes)
    }

    override fun <V : FileAttributeView> getFileAttributeView(path: Path, type: Class<V>, vararg options: LinkOption): V? {
        return delegateProvider().getFileAttributeView(path.toDelegatePath(), type, *options)
    }

    override fun <A : BasicFileAttributes> readAttributes(path: Path, type: Class<A>, vararg options: LinkOption): A {
        return delegateProvider().readAttributes(path.toDelegatePath(), type, *options)
    }

    override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> {
        return delegateProvider().readAttributes(path.toDelegatePath(), attributes, *options)
    }

    override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) {
        delegateProvider().setAttribute(path.toDelegatePath(), attribute, value, *options)
    }

    private fun requireRegisteredFileSystem(): SharpFileSystem {
        return fileSystem ?: throw IllegalStateException("No test filesystem is registered in the provider")
    }

    private fun delegateProvider(): FileSystemProvider = requireRegisteredFileSystem().delegateFileSystem.provider()
}

private class SharpFileSystem(
    private val provider: SharpFileSystemProvider,
    val delegateFileSystem: FileSystem,
) : FileSystem() {
    companion object {
        const val PATH_SEPARATOR = "#"
    }

    override fun provider(): FileSystemProvider = provider

    override fun close() = Unit

    override fun isOpen(): Boolean = delegateFileSystem.isOpen

    override fun isReadOnly(): Boolean = delegateFileSystem.isReadOnly

    override fun getSeparator(): String = PATH_SEPARATOR

    override fun getRootDirectories(): MutableIterable<Path> =
        delegateFileSystem.rootDirectories.mapTo(ArrayList()) { wrap(it) }

    override fun getFileStores(): MutableIterable<FileStore> = delegateFileSystem.fileStores

    override fun supportedFileAttributeViews(): MutableSet<String> =
        delegateFileSystem.supportedFileAttributeViews()

    override fun getPath(first: String, vararg more: String): Path {
        val convertedFirst = first.fromTestSeparators()
        val convertedMore = more.map { it.fromTestSeparators() }.toTypedArray()
        return wrap(delegateFileSystem.getPath(convertedFirst, *convertedMore))
    }

    override fun getPathMatcher(syntaxAndPattern: String): PathMatcher = TODO()

    override fun getUserPrincipalLookupService(): UserPrincipalLookupService =
        delegateFileSystem.userPrincipalLookupService

    override fun newWatchService(): WatchService = TODO()
    fun wrap(delegatePath: Path): SharpPath = SharpPath(this, delegatePath)

    private fun String.fromTestSeparators(): String =
        replace(PATH_SEPARATOR, delegateFileSystem.separator)
}

private class SharpPath(
    val fileSystem: SharpFileSystem,
    val delegatePath: Path,
) : Path {
    override fun getFileSystem(): FileSystem = fileSystem

    override fun isAbsolute(): Boolean = delegatePath.isAbsolute

    override fun getRoot(): Path? = delegatePath.root?.let(fileSystem::wrap)

    override fun getFileName(): Path? = delegatePath.fileName?.let(fileSystem::wrap)

    override fun getParent(): Path? = delegatePath.parent?.let(fileSystem::wrap)

    override fun getNameCount(): Int = delegatePath.nameCount

    override fun getName(index: Int): Path = fileSystem.wrap(delegatePath.getName(index))

    override fun subpath(beginIndex: Int, endIndex: Int): Path = fileSystem.wrap(delegatePath.subpath(beginIndex, endIndex))

    override fun startsWith(other: Path): Boolean = other is SharpPath && delegatePath.startsWith(other.delegatePath)

    override fun startsWith(other: String): Boolean = startsWith(fileSystem.getPath(other))

    override fun endsWith(other: Path): Boolean = other is SharpPath && delegatePath.endsWith(other.delegatePath)

    override fun endsWith(other: String): Boolean = endsWith(fileSystem.getPath(other))

    override fun normalize(): Path = fileSystem.wrap(delegatePath.normalize())

    override fun resolve(other: Path): Path {
        other as SharpPath
        return fileSystem.wrap(delegatePath.resolve(other.delegatePath))
    }

    override fun resolve(other: String): Path = resolve(fileSystem.getPath(other))

    override fun resolveSibling(other: Path): Path {
        other as SharpPath
        return fileSystem.wrap(delegatePath.resolveSibling(other.delegatePath))
    }

    override fun resolveSibling(other: String): Path = resolveSibling(fileSystem.getPath(other))

    override fun relativize(other: Path): Path = fileSystem.wrap(delegatePath.relativize((other as SharpPath).delegatePath))

    override fun toUri(): URI = URI("${fileSystem.provider().scheme}:///${toString()}")

    override fun toAbsolutePath(): Path = fileSystem.wrap(delegatePath.toAbsolutePath())

    override fun toRealPath(vararg options: LinkOption): Path = fileSystem.wrap(delegatePath.toRealPath(*options))

    override fun toFile(): File = delegatePath.toFile()

    override fun register(
        watcher: WatchService,
        events: Array<out WatchEvent.Kind<*>>,
        vararg modifiers: WatchEvent.Modifier,
    ): WatchKey = TODO()

    override fun register(watcher: WatchService, vararg events: WatchEvent.Kind<*>): WatchKey = TODO()

    override fun iterator(): MutableIterator<Path> =
        delegatePath.mapTo(ArrayList()) { fileSystem.wrap(it) }.iterator()

    override fun compareTo(other: Path): Int = delegatePath.compareTo((other as SharpPath).delegatePath)

    override fun equals(other: Any?): Boolean =
        other is SharpPath && fileSystem == other.fileSystem && delegatePath == other.delegatePath

    override fun hashCode(): Int = 31 * fileSystem.hashCode() + delegatePath.hashCode()

    override fun toString(): String =
        delegatePath.toString().replace(fileSystem.delegateFileSystem.separator, SharpFileSystem.PATH_SEPARATOR)
}

private fun Path.toDelegatePath(): Path = (this as SharpPath).delegatePath
