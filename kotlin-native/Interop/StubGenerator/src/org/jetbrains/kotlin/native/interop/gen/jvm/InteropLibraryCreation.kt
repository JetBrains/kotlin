/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.backend.common.includeSignatureIndex
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_ARM64E_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS
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
import java.nio.file.Path
import java.util.*

fun createInteropLibrary(
        serializedMetadata: SerializedMetadata,
        outputPath: Path,
        moduleName: String,
        nativeBitcodeFiles: List<Path>,
        arm64eBitcodeFiles: List<Path> = emptyList(),
        target: KonanTarget,
        manifest: Properties,
        dependencies: List<KotlinLibrary>,
        nopack: Boolean,
        shortName: String?,
        staticLibraries: List<Path>,
        klibAbiCompatibilityLevel: KlibAbiCompatibilityLevel,
        topLevelSignatures: ExtractedSignatures,
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
                if (arm64eBitcodeFiles.isNotEmpty()) setProperty(KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS, KLIB_ARM64E_FOLDER_NAME)
            }
            legacyNativeShortNameInManifest(shortName)
            legacyNativeDependenciesInManifest(dependencies.map { it.uniqueName })
        }
        includeMetadata(serializedMetadata)
        includeBitcode(target, nativeBitcodeFiles)
        if (arm64eBitcodeFiles.isNotEmpty()) includeBitcode(target, arm64eBitcodeFiles, arm64e = true)
        includeNativeIncludedBinaries(target, staticLibraries)
        includeSignatureIndex(topLevelSignatures)
    }.writeTo(outputPath)
}
