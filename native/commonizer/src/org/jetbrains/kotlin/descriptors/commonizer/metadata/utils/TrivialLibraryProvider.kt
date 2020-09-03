/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata.utils

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.library.MetadataLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

/**
 * Provides access to metadata using default compiler's routine.
 */
// TODO: move to a separate module (kotlin-native-utils-metadata?) to share with C-interop tool?
class TrivialLibraryProvider(
    private val library: MetadataLibrary
) : KlibModuleMetadata.MetadataLibraryProvider {

    override val moduleHeaderData: ByteArray
        get() = library.moduleHeaderData

    override fun packageMetadata(fqName: String, partName: String): ByteArray =
        library.packageMetadata(fqName, partName)

    override fun packageMetadataParts(fqName: String): Set<String> =
        library.packageMetadataParts(fqName)

    companion object {
        fun readLibraryMetadata(libraryPath: File): KlibModuleMetadata {
            check(libraryPath.exists()) { "Library does not exist: $libraryPath" }

            val library = resolveSingleFileKlib(KFile(libraryPath.absolutePath), strategy = ToolingSingleFileKlibResolveStrategy)
            return KlibModuleMetadata.read(TrivialLibraryProvider(library))
        }
    }
}
