package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.parseKonanVersion
import org.jetbrains.kotlin.konan.KonanAbiVersion
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.parseKonanAbiVersion


data class KonanLibraryVersioning(
    val libraryVersion: String?,
    val compilerVersion: KonanVersion,
    val abiVersion: KonanAbiVersion
)

fun Properties.writeKonanLibraryVersioning(versions: KonanLibraryVersioning) {
    this.setProperty(KLIB_PROPERTY_ABI_VERSION, versions.abiVersion.toString())
    versions.libraryVersion ?. let { this.setProperty(KLIB_PROPERTY_LIBRARY_VERSION, it) }
    this.setProperty(KLIB_PROPERTY_COMPILER_VERSION, "${versions.compilerVersion}")
}

fun Properties.readKonanLibraryVersioning(): KonanLibraryVersioning {
    val abiVersion = this.getProperty(KLIB_PROPERTY_ABI_VERSION)!!.parseKonanAbiVersion()
    val libraryVersion = this.getProperty(KLIB_PROPERTY_LIBRARY_VERSION)
    val compilerVersion = this.getProperty(KLIB_PROPERTY_COMPILER_VERSION)!!.parseKonanVersion()

    return KonanLibraryVersioning(abiVersion = abiVersion, libraryVersion = libraryVersion, compilerVersion = compilerVersion)
}