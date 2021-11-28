package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*

const val KLIB_PROPERTY_LINKED_OPTS = "linkerOpts"
const val KLIB_PROPERTY_INCLUDED_HEADERS = "includedHeaders"

interface TargetedLibrary {
    val targetList: List<String>
    val includedPaths: List<String>
}

interface BitcodeLibrary : TargetedLibrary {
    val bitcodePaths: List<String>
}

interface KonanLibrary : BitcodeLibrary, KotlinLibrary {
    val linkerOpts: List<String>
}

val KonanLibrary.includedHeaders
    get() = manifestProperties.propertyList(KLIB_PROPERTY_INCLUDED_HEADERS, escapeInQuotes = true)
