/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.toSpaceSeparatedString
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.isCommonizedCInteropLibrary

/**
 * The set of properties in manifest of Kotlin/Native library that should be
 * preserved in commonized libraries (both for "common" and platform-specific library parts).
 */
data class NativeSensitiveManifestData(
    val uniqueName: UniqueLibraryName,
    val versions: KotlinLibraryVersioning,
    val dependencies: List<String>,
    val isCInterop: Boolean,
    val packageFqName: String?,
    val exportForwardDeclarations: List<String>,
    val includedForwardDeclarations: List<String>,
    val nativeTargets: Collection<String>,
    val shortName: String?,
    val commonizerTarget: CommonizerTarget?,
) {

    companion object {
        fun readFrom(library: KotlinLibrary) = NativeSensitiveManifestData(
            uniqueName = library.uniqueName,
            versions = library.versions,
            dependencies = library.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true),
            isCInterop = library.isCInteropLibrary() || library.isCommonizedCInteropLibrary(),
            packageFqName = library.packageFqName,
            exportForwardDeclarations = library.exportForwardDeclarations,
            includedForwardDeclarations = library.includedForwardDeclarations,
            nativeTargets = library.nativeTargets,
            shortName = library.shortName,
            commonizerTarget = library.commonizerTarget?.let(::parseCommonizerTargetOrNull),
        )
    }
}

private inline fun BaseWriterImpl.addOptionalProperty(name: String, condition: Boolean, value: () -> String) {
    if (condition) manifestProperties[name] = value()
    else manifestProperties.remove(name)
}

fun BaseWriterImpl.addManifest(manifest: NativeSensitiveManifestData) {
    manifestProperties[KLIB_PROPERTY_UNIQUE_NAME] = manifest.uniqueName

    // note: versions can't be added here
    // Make sure all the lists are sorted for reproducible output

    addOptionalProperty(KLIB_PROPERTY_DEPENDS, manifest.dependencies.isNotEmpty()) {
        manifest.dependencies.sorted().toSpaceSeparatedString()
    }

    /*
     * Note: The `ir_provider` property is not written to manifest because:
     *  1. Commonized libraries were never supposed to provide any IR.
     *  2. It has not been tested, we don't know if it works at all.
     */
    addOptionalProperty(KLIB_PROPERTY_INTEROP, manifest.isCInterop) { "true" }
    addOptionalProperty(KLIB_PROPERTY_PACKAGE, manifest.packageFqName != null) { manifest.packageFqName!! }
    addOptionalProperty(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS, manifest.exportForwardDeclarations.isNotEmpty() || manifest.isCInterop) {
        manifest.exportForwardDeclarations.sorted().joinToString(" ")
    }

    addOptionalProperty(KLIB_PROPERTY_INCLUDED_FORWARD_DECLARATIONS, manifest.includedForwardDeclarations.isNotEmpty() || manifest.isCInterop) {
        manifest.includedForwardDeclarations.sorted().joinToString(" ")
    }

    addOptionalProperty(KLIB_PROPERTY_NATIVE_TARGETS, manifest.nativeTargets.isNotEmpty()) {
        manifest.nativeTargets.sorted().joinToString(" ")
    }

    addOptionalProperty(KLIB_PROPERTY_SHORT_NAME, manifest.shortName != null) { manifest.shortName!! }

    addOptionalProperty(KLIB_PROPERTY_COMMONIZER_TARGET, manifest.commonizerTarget != null) {
        manifest.commonizerTarget?.identityString ?: error("Unexpected missing 'commonizerTarget'")
    }

    addOptionalProperty(KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS, manifest.commonizerTarget != null) {
        manifest.commonizerTarget?.konanTargets?.map { it.name }?.sorted()?.joinToString(" ")
            ?: error("Unexpected missing 'commonizerTarget'")
    }
}
