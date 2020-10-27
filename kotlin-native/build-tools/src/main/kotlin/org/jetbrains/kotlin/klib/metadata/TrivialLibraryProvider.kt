/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.klib.metadata

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.library.MetadataLibrary

/**
 * Provides access to metadata using default compiler's routine.
 */
internal class TrivialLibraryProvider(
        private val library: MetadataLibrary
) : KlibModuleMetadata.MetadataLibraryProvider {

    override val moduleHeaderData: ByteArray
        get() = library.moduleHeaderData

    override fun packageMetadata(fqName: String, partName: String): ByteArray =
            library.packageMetadata(fqName, partName)

    override fun packageMetadataParts(fqName: String): Set<String> =
            library.packageMetadataParts(fqName)
}