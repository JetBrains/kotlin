/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.asZipRoot
import org.jetbrains.kotlin.konan.file.createTempDir
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.target.KonanTarget

private class ZippedKonanLibraryLayout(val klibFile: File, override val target: KonanTarget?) : KonanLibraryLayout {

    init {
        zippedKonanLibraryChecks(klibFile)
    }

    override val libraryName = klibFile.path.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT)

    override val libDir by lazy { zippedKonanLibraryRoot(klibFile) }
}

internal fun zippedKonanLibraryChecks(klibFile: File) {
    check(klibFile.exists) { "Could not find $klibFile." }
    check(klibFile.isFile) { "Expected $klibFile to be a regular file." }

    val extension = klibFile.extension
    check(extension.isEmpty() || extension == KLIB_FILE_EXTENSION) { "Unexpected file extension: $extension" }
}

internal fun zippedKonanLibraryRoot(klibFile: File) = klibFile.asZipRoot

private class UnzippedKonanLibraryLayout(override val libDir: File, override val target: KonanTarget?) : KonanLibraryLayout {
    override val libraryName = libDir.path
}

/**
 * This class automatically extracts pieces of the library on first access. Use it if you need
 * to pass extracted files to an external tool. Otherwise, stick to [ZippedKonanLibraryLayout].
 */
private class FileExtractor(zippedLibraryLayout: KonanLibraryLayout) : KonanLibraryLayout by zippedLibraryLayout {

    override val manifestFile: File by lazy { extract(super.manifestFile) }

    override val resourcesDir: File by lazy { extractDir(super.resourcesDir) }

    override val includedDir: File by lazy { extractDir(super.includedDir) }

    override val kotlinDir: File by lazy { extractDir(super.kotlinDir) }

    override val nativeDir: File by lazy { extractDir(super.nativeDir) }

    override val linkdataDir: File by lazy { extractDir(super.linkdataDir) }

    fun extract(file: File): File {
        val temporary = createTempFile(file.name)
        file.copyTo(temporary)
        temporary.deleteOnExit()
        return temporary
    }

    fun extractDir(directory: File): File {
        val temporary = createTempDir(directory.name)
        directory.recursiveCopyTo(temporary)
        temporary.deleteOnExitRecursively()
        return temporary
    }
}

internal fun createKonanLibraryLayout(klib: File, target: KonanTarget? = null) =
    if (klib.isFile) ZippedKonanLibraryLayout(klib, target) else UnzippedKonanLibraryLayout(klib, target)

internal val KonanLibraryLayout.realFiles
    get() = when (this) {
        is ZippedKonanLibraryLayout -> FileExtractor(this)
        // Unpacked library just provides its own files.
        is UnzippedKonanLibraryLayout -> this
        else -> error("Provide an extractor for your container.")
    }

