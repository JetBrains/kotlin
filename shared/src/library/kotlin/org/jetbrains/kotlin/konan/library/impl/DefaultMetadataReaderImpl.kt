package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.library.MetadataReader

internal object DefaultMetadataReaderImpl : MetadataReader {

    override fun loadSerializedModule(libraryLayout: KonanLibraryLayout): ByteArray =
            libraryLayout.moduleHeaderFile.readBytes()

    override fun loadSerializedPackageFragment(libraryLayout: KonanLibraryLayout, fqName: String, partName: String): ByteArray =
            libraryLayout.packageFragmentFile(fqName, partName).readBytes()

}
