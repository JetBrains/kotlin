package org.jetbrains.kotlin.konan.library

interface MetadataReader {
    fun loadSerializedModule(libraryLayout: KonanLibraryLayout): ByteArray
    fun loadSerializedPackageFragment(libraryLayout: KonanLibraryLayout, fqName: String, partName: String): ByteArray
}
