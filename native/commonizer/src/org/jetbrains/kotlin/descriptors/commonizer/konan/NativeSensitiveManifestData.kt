/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BaseWriterImpl

/**
 * The set of properties in manifest of Kotlin/Native library that should be
 * preserved in commonized libraries (both for "common" and platform-specific library parts).
 */
internal data class NativeSensitiveManifestData(
    val uniqueName: String,
    val versions: KotlinLibraryVersioning,
    val dependencies: List<String>,
    val isInterop: Boolean,
    val packageFqName: String?,
    val exportForwardDeclarations: List<String>,
    val nativeTargets: Collection<String>,
    val shortName: String?
) {
    fun applyTo(library: BaseWriterImpl) {
        library.manifestProperties[KLIB_PROPERTY_UNIQUE_NAME] = uniqueName

        // note: versions can't be added here

        fun addOptionalProperty(name: String, condition: Boolean, value: () -> String) =
            if (condition)
                library.manifestProperties[name] = value()
            else
                library.manifestProperties.remove(name)

        addOptionalProperty(KLIB_PROPERTY_DEPENDS, dependencies.isNotEmpty()) { dependencies.joinToString(separator = " ") }
        addOptionalProperty(KLIB_PROPERTY_INTEROP, isInterop) { "true" }
        addOptionalProperty(KLIB_PROPERTY_PACKAGE, packageFqName != null) { packageFqName!! }
        addOptionalProperty(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS, exportForwardDeclarations.isNotEmpty()) {
            exportForwardDeclarations.joinToString(" ")
        }
        addOptionalProperty(KLIB_PROPERTY_NATIVE_TARGETS, nativeTargets.isNotEmpty()) {
            nativeTargets.joinToString(" ")
        }
        addOptionalProperty(KLIB_PROPERTY_SHORT_NAME, shortName != null) { shortName!! }
    }

    fun mergeWith(other: NativeSensitiveManifestData): NativeSensitiveManifestData {
        if (this === other) return this

        check(uniqueName == other.uniqueName)

        // Assumption: It's enough to merge native targets list, other properties can be taken from 'this' manifest.

        return NativeSensitiveManifestData(
            uniqueName = uniqueName,
            versions = versions,
            dependencies = (dependencies union other.dependencies).toList(),
            isInterop = isInterop,
            packageFqName = packageFqName,
            exportForwardDeclarations = exportForwardDeclarations,
            nativeTargets = HashSet<String>().apply {
                addAll(nativeTargets)
                addAll(other.nativeTargets)
            },
            shortName = shortName
        )
    }

    companion object {
        fun readFrom(library: KotlinLibrary) = NativeSensitiveManifestData(
            uniqueName = library.uniqueName,
            versions = library.versions,
            dependencies = library.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true),
            isInterop = library.isInterop,
            packageFqName = library.packageFqName,
            exportForwardDeclarations = library.exportForwardDeclarations,
            nativeTargets = library.nativeTargets,
            shortName = library.shortName
        )
    }
}
