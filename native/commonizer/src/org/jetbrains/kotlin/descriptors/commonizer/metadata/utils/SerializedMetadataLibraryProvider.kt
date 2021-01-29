/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata.utils

import kotlinx.metadata.klib.KlibModuleMetadata

class SerializedMetadataLibraryProvider(
    override val moduleHeaderData: ByteArray,
    fragments: List<List<ByteArray>>,
    fragmentNames: List<String>
) : KlibModuleMetadata.MetadataLibraryProvider {
    private val fragmentMap: Map<String, Map<String, ByteArray>>

    init {
        check(fragments.size == fragmentNames.size)

        fragmentMap = fragmentNames.mapIndexed { fragmentIndex, fragmentName ->
            // fragmentName is package FQ name, fragmentShortName is right-most part of package FQ name
            val fragmentShortName = fragmentName.substringAfterLast('.')

            val fragmentParts = fragments[fragmentIndex]
            val digitCount = fragmentParts.size.toString().length

            // N.B. the same fragment part numbering scheme as in org.jetbrains.kotlin.library.impl.MetadataWriterImpl
            val fragmentPartMap = fragmentParts.mapIndexed { partIndex, part ->
                val partName = partIndex.toString().padStart(digitCount, '0') + "_" + fragmentShortName
                partName to part
            }.toMap()

            fragmentName to fragmentPartMap
        }.toMap()
    }

    override fun packageMetadataParts(fqName: String): Set<String> {
        return fragmentMap.getValue(fqName).keys
    }

    override fun packageMetadata(fqName: String, partName: String): ByteArray {
        return fragmentMap.getValue(fqName).getValue(partName)
    }
}
