/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.library.writer.includeBitcode
import org.jetbrains.kotlin.konan.library.writer.includeNativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.writer.legacyNativeDependenciesInManifest
import org.jetbrains.kotlin.konan.library.writer.legacyNativeShortNameInManifest
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KlibFormat
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.util.toCInteropKlibMetadataVersion
import java.util.*

fun createInteropLibrary(
    serializedMetadata: SerializedMetadata,
    outputPath: String,
    moduleName: String,
    nativeBitcodeFiles: List<String>,
    target: KonanTarget,
    manifest: Properties,
    dependencies: List<KotlinLibrary>,
    nopack: Boolean,
    shortName: String?,
    staticLibraries: List<String>,
    klibAbiCompatibilityLevel: KlibAbiCompatibilityLevel,
) {
    KlibWriter {
        format(if (nopack) KlibFormat.Directory else KlibFormat.ZipArchive)
        manifest {
            moduleName(moduleName)
            versions(
                    KotlinLibraryVersioning(
                            abiVersion = klibAbiCompatibilityLevel.toAbiVersionForManifest(),
                            compilerVersion = KotlinCompilerVersion.VERSION,
                            metadataVersion = klibAbiCompatibilityLevel.toCInteropKlibMetadataVersion(),
                    )
            )
            platformAndTargets(BuiltInsPlatform.NATIVE, target.visibleName)
            customProperties {
                this += (manifest - KLIB_PROPERTY_DEPENDS) // Do not propagate the `depends=` property from *.def files, rely on `dependencies` instead.
            }
            legacyNativeShortNameInManifest(shortName)
            legacyNativeDependenciesInManifest(dependencies.map { it.uniqueName })
        }
        includeMetadata(serializedMetadata)
        includeBitcode(target, nativeBitcodeFiles)
        includeNativeIncludedBinaries(target, staticLibraries)
    }.writeTo(outputPath)
}
