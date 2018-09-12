package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute


class KonanLibraryImpl(
        override val libraryFile: File,
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

    override val versions: KonanLibraryVersioning
        get() = manifestProperties.readKonanLibraryVersioning()

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList(KLIB_PROPERTY_LINKED_OPTS, target!!.visibleName)

    override val bitcodePaths: List<String>
        get() = layout.realFiles { (it.kotlinDir.listFilesOrEmpty + it.nativeDir.listFilesOrEmpty).map { it.absolutePath } }

    override val includedPaths: List<String>
        get() = layout.realFiles { it.includedDir.listFilesOrEmpty.map { it.absolutePath } }

    override val targetList by lazy { layout.inPlace { it.targetsDir.listFiles.map { it.name } } }

    override val dataFlowGraph by lazy { layout.inPlace { it.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null } } }

    override val moduleHeaderData: ByteArray by lazy { layout.inPlace { metadataReader.loadSerializedModule(it) } }

    override fun packageMetadata(fqName: String) = layout.inPlace { metadataReader.loadSerializedPackageFragment(it, fqName) }

    override fun toString() = "$libraryName[default=$isDefault]"
}