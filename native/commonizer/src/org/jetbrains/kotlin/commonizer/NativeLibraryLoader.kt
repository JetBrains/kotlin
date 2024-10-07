/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cli.errorAndExitJvmProcess
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.util.Logger
import java.io.File

internal fun interface NativeLibraryLoader {
    operator fun invoke(file: File): NativeLibrary
}

internal class DefaultNativeLibraryLoader(
    private val logger: Logger
) : NativeLibraryLoader {
    override fun invoke(file: File): NativeLibrary {
        try {
            val library = resolveSingleFileKlib(
                libraryFile = org.jetbrains.kotlin.konan.file.File(file.path),
                logger = logger,
                strategy = ToolingSingleFileKlibResolveStrategy
            )

            if (library.versions.metadataVersion == null)
                logger.errorAndExitJvmProcess("Library does not have metadata version specified in manifest: $file")

            return NativeLibrary(library)
        } catch (cause: Throwable) {
            throw NativeLibraryLoadingException(
                "Failed loading library at ${file.path}", cause
            )
        }
    }
}

internal class NativeLibraryLoadingException(message: String, cause: Throwable? = null) : Exception(message, cause)
