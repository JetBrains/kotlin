/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.konan.library.KLIB_PROPERTY_LINKED_OPTS
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.MetadataReader
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute

internal class KonanLibraryImpl(
    override val libraryFile: File,
    private val currentAbiVersion: Int,
    internal val target: KonanTarget?,
    override val isDefault: Boolean,
    private val metadataReader: MetadataReader
) : KonanLibrary {

    // For the zipped libraries inPlace gives files from zip file system
    // whereas realFiles extracts them to /tmp.
    // For unzipped libraries inPlace and realFiles are the same
    // providing files in the library directory.
    private val inPlace = createKonanLibraryLayout(libraryFile, target)
    private val realFiles = inPlace.realFiles

    override val libraryName
        get() = inPlace.libraryName

    override val manifestProperties: Properties by lazy {
        val properties = inPlace.manifestFile.loadProperties()
        if (target != null) substitute(properties, defaultTargetSubstitutions(target))
        properties
    }

    override val abiVersion: String
        get() {
            val manifestAbiVersion = manifestProperties.getProperty(KLIB_PROPERTY_ABI_VERSION)
            check(currentAbiVersion.toString() == manifestAbiVersion) {
                "ABI version mismatch. Compiler expects: $currentAbiVersion, the library is $manifestAbiVersion"
            }
            return manifestAbiVersion
        }

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList(KLIB_PROPERTY_LINKED_OPTS, target!!.visibleName)

    override val bitcodePaths: List<String>
        get() = (realFiles.kotlinDir.listFilesOrEmpty + realFiles.nativeDir.listFilesOrEmpty).map { it.absolutePath }

    override val includedPaths: List<String>
        get() = realFiles.includedDir.listFilesOrEmpty.map { it.absolutePath }

    override val targetList by lazy { inPlace.targetsDir.listFiles.map { it.name } }

    override val dataFlowGraph by lazy { inPlace.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null } }

    override val moduleHeaderData: ByteArray by lazy { metadataReader.loadSerializedModule(inPlace) }

    override fun packageMetadata(fqName: String) = metadataReader.loadSerializedPackageFragment(inPlace, fqName)

    override fun toString() = "$libraryName[default=$isDefault]"
}
