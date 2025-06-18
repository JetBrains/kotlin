/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.commonizer.cli.CliLoggerAdapter
import org.jetbrains.kotlin.commonizer.cli.errorAndExitJvmProcess
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import java.io.File

internal class NativeLibraryLoader(private val logger: CliLoggerAdapter) {
    fun loadLibrary(libraryFile: File): NativeLibrary {
        val result = KlibLoader { libraryPaths(libraryFile.path) }.load()
        result.reportLoadingProblemsIfAny(logger, allAsErrors = true)

        val library = result.librariesStdlibFirst.single()
        if (library.versions.metadataVersion == null)
            logger.errorAndExitJvmProcess("Library does not have metadata version specified in manifest: $libraryFile")

        return NativeLibrary(library)

    }
}
