package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.library.MetadataReader

internal object DefaultMetadataReaderImpl : MetadataReader {

    override fun loadSerializedModule(libraryLayout: KonanLibraryLayout): ByteArray =
            libraryLayout.moduleHeaderFile.readBytes()

    override fun loadSerializedPackageFragment(libraryLayout: KonanLibraryLayout, fqName: String, partName: String): ByteArray =
            libraryLayout.packageFragmentFile(fqName, partName).readBytes()

    override fun loadIrHeader(libraryLayout: KonanLibraryLayout): ByteArray =
            libraryLayout.irHeader.readBytes()

    override fun loadIrDeclaraton(libraryLayout: KonanLibraryLayout, index: Long, isLocal: Boolean): ByteArray {
        val name = index.toULong().toString(16)
        val file = if (isLocal)
            libraryLayout.hiddenDeclarationFile(name)
        else
            libraryLayout.visibleDeclarationFile(name)
        return file.readBytes()
    }
}
