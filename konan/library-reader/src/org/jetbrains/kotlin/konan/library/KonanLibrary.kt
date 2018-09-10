/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.name.FqName

const val KLIB_PROPERTY_ABI_VERSION = "abi_version"
const val KLIB_PROPERTY_UNIQUE_NAME = "unique_name"
const val KLIB_PROPERTY_LINKED_OPTS = "linkerOpts"
const val KLIB_PROPERTY_DEPENDS = "depends"
const val KLIB_PROPERTY_INTEROP = "interop"
const val KLIB_PROPERTY_PACKAGE = "package"
const val KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS = "exportForwardDeclarations"
const val KLIB_PROPERTY_INCLUDED_HEADERS = "includedHeaders"

/**
 * An abstraction for getting access to the information stored inside of Kotlin/Native library.
 */
interface KonanLibrary {

    val libraryName: String
    val libraryFile: File

    // Whether this library is default (provided by Kotlin/Native distribution)?
    val isDefault: Boolean

    // Properties:
    val manifestProperties: Properties
    val abiVersion: String
    val linkerOpts: List<String>

    // Paths:
    val bitcodePaths: List<String>
    val includedPaths: List<String>

    val targetList: List<String>

    val dataFlowGraph: ByteArray?
    val moduleHeaderData: KonanProtoBuf.LinkDataLibrary
    fun packageMetadata(packageFqName: String): KonanProtoBuf.LinkDataPackageFragment
}

val KonanLibrary.uniqueName
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)!!

val KonanLibrary.unresolvedDependencies: List<String>
    get() = manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS)

val KonanLibrary.isInterop
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INTEROP) == "true"

val KonanLibrary.packageFqName
    get() = manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)?.let { FqName(it) }

val KonanLibrary.exportForwardDeclarations
    get() = manifestProperties.getProperty(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS)
        .split(' ').asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { FqName(it) }
        .toList()

val KonanLibrary.includedHeaders
    get() = manifestProperties.getProperty(KLIB_PROPERTY_INCLUDED_HEADERS).split(' ')
