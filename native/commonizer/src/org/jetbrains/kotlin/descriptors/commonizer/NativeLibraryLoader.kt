/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeLibrary
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
        val library = resolveSingleFileKlib(
            libraryFile = org.jetbrains.kotlin.konan.file.File(file.path),
            logger = logger,
            strategy = ToolingSingleFileKlibResolveStrategy
        )

        if (library.versions.metadataVersion == null)
            logger.fatal("Library does not have metadata version specified in manifest: $file")

        val metadataVersion = library.metadataVersion
        if (metadataVersion?.isCompatible() != true)
            logger.fatal(
                """
                Library has incompatible metadata version ${metadataVersion ?: "\"unknown\""}: $file
                Please make sure that all libraries passed to commonizer compatible metadata version ${KlibMetadataVersion.INSTANCE}
                """.trimIndent()
            )

        return NativeLibrary(library)
    }
}
