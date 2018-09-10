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
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf

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

    private val layout = createKonanLibraryLayout(libraryFile, target)

    override val libraryName: String by lazy { layout.inPlace { it.libraryName } }

    override val manifestProperties: Properties by lazy {
        val properties = layout.inPlace { it.manifestFile.loadProperties() }
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
        get() = layout.realFiles { (it.kotlinDir.listFilesOrEmpty + it.nativeDir.listFilesOrEmpty).map { it.absolutePath } }

    override val includedPaths: List<String>
        get() = layout.realFiles { it.includedDir.listFilesOrEmpty.map { it.absolutePath } }

    override val targetList by lazy { layout.inPlace { it.targetsDir.listFiles.map { it.name } } }

    override val dataFlowGraph by lazy { layout.inPlace { it.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null } } }

    override val moduleHeaderData: KonanProtoBuf.LinkDataLibrary by lazy { layout.inPlace { metadataReader.loadSerializedModule(it) } }

    override fun packageMetadata(packageFqName: String) = layout.inPlace { metadataReader.loadSerializedPackageFragment(it, packageFqName) }

    override fun toString() = "$libraryName[default=$isDefault]"
}
