package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KotlinLibrary

const val KLIB_PROPERTY_LINKED_OPTS = "linkerOpts"
const val KLIB_PROPERTY_INCLUDED_HEADERS = "includedHeaders"

interface TargetedLibrary {
    val targetList: List<String>
}

interface KonanLibrary : KotlinLibrary, TargetedLibrary {
    val linkerOpts: List<String>
}

val KonanLibrary.includedHeaders
    get() = manifestProperties.propertyList(KLIB_PROPERTY_INCLUDED_HEADERS, escapeInQuotes = true)
