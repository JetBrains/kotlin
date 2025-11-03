package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.commonizerNativeTargets
import org.jetbrains.kotlin.library.nativeTargets

const val KLIB_PROPERTY_LINKED_OPTS = "linkerOpts"
const val KLIB_PROPERTY_INCLUDED_HEADERS = "includedHeaders"

interface TargetedLibrary

interface KonanLibrary : KotlinLibrary, TargetedLibrary

val KonanLibrary.includedHeaders
    get() = manifestProperties.propertyList(KLIB_PROPERTY_INCLUDED_HEADERS, escapeInQuotes = true)

val KonanLibrary.supportedTargetList: List<String>
    get() = commonizerNativeTargets?.takeIf(Collection<String>::isNotEmpty) ?: nativeTargets

val KonanLibrary.linkerOpts: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_LINKED_OPTS, escapeInQuotes = true)
