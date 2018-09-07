/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.FileSystem

interface KonanLibraryLayoutImpl : KonanLibraryLayout {
    fun <T> inPlace(action: (KonanLibraryLayout) -> T): T
    fun <T> realFiles(action: (KonanLibraryLayout) -> T): T
}

private class ZippedKonanLibraryLayout(val klibFile: File, override val target: KonanTarget?) : KonanLibraryLayoutImpl {

    init {
        zippedKonanLibraryChecks(klibFile)
    }

    override val libraryName = klibFile.path.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT)

    override val libDir: File = File("/")

    override fun <T> realFiles(action: (KonanLibraryLayout) -> T): T {
        return action(FileExtractor(this))!!
    }

    override fun <T> inPlace(action: (KonanLibraryLayout) -> T): T {
        return klibFile.withZipFileSystem { zipFileSystem ->
            action(DirectFromZip(this, zipFileSystem))
        }
    }
}

internal fun zippedKonanLibraryChecks(klibFile: File) {
    check(klibFile.exists) { "Could not find $klibFile." }
    check(klibFile.isFile) { "Expected $klibFile to be a regular file." }

    val extension = klibFile.extension
    check(extension.isEmpty() || extension == KLIB_FILE_EXTENSION) { "Unexpected file extension: $extension" }
}

private class UnzippedKonanLibraryLayout(override val libDir: File, override val target: KonanTarget?) : KonanLibraryLayoutImpl {
    override val libraryName = libDir.path

    override fun <T> inPlace(action: (KonanLibraryLayout) -> T): T = action(this)
    override fun <T> realFiles(action: (KonanLibraryLayout) -> T): T = inPlace(action)
}

private class DirectFromZip(zippedLayout: ZippedKonanLibraryLayout, val zipFileSystem: FileSystem) : KonanLibraryLayout {
    override val libraryName = zippedLayout.libraryName
    override val libDir = zipFileSystem.file(zippedLayout.libDir)
}

/**
 * This class automatically extracts pieces of the library on first access. Use it if you need
 * to pass extracted files to an external tool. Otherwise, stick to [DirectFromZip].
 */
private class FileExtractor(val zippedLibraryLayout: ZippedKonanLibraryLayout) : KonanLibraryLayout by zippedLibraryLayout {

    override val manifestFile: File by lazy { extract(super.manifestFile) }

    override val resourcesDir: File by lazy { extractDir(super.resourcesDir) }

    override val includedDir: File by lazy { extractDir(super.includedDir) }

    override val kotlinDir: File by lazy { extractDir(super.kotlinDir) }

    override val nativeDir: File by lazy { extractDir(super.nativeDir) }

    override val linkdataDir: File by lazy { extractDir(super.linkdataDir) }

    fun extract(file: File): File = zippedLibraryLayout.klibFile.withZipFileSystem { zipFileSystem ->
        val temporary = createTempFile(file.name)
        zipFileSystem.file(file).copyTo(temporary)
        temporary.deleteOnExit()
        temporary
    }

    fun extractDir(directory: File): File = zippedLibraryLayout.klibFile.withZipFileSystem { zipFileSystem ->
        val temporary = createTempDir(directory.name)
        zipFileSystem.file(directory).recursiveCopyTo(temporary)
        temporary.deleteOnExitRecursively()
        temporary
    }
}

internal fun createKonanLibraryLayout(klib: File, target: KonanTarget? = null) =
    if (klib.isFile) ZippedKonanLibraryLayout(klib, target) else UnzippedKonanLibraryLayout(klib, target)

