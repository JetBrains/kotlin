package org.jetbrains.kotlin.konan.library

interface MetadataReader {
    fun loadSerializedModule(libraryLayout: KonanLibraryLayout): ByteArray
    fun loadSerializedPackageFragment(libraryLayout: KonanLibraryLayout, fqName: String, partName: String): ByteArray
    fun loadIrHeader(libraryLayout: KonanLibraryLayout): ByteArray
    fun loadIrDeclaraton(libraryLayout: KonanLibraryLayout, index: Long, isLocal: Boolean): ByteArray
}
