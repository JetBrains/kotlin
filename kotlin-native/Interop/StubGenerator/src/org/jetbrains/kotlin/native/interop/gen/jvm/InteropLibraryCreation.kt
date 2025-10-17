/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
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
    buildLibrary(
            natives = nativeBitcodeFiles,
            included = staticLibraries,
            linkDependencies = dependencies,
            metadata = serializedMetadata,
            ir = null,
            versions = KotlinLibraryVersioning(
                    abiVersion = klibAbiCompatibilityLevel.toAbiVersionForManifest(),
                    compilerVersion = KotlinCompilerVersion.VERSION,
                    metadataVersion = klibAbiCompatibilityLevel.toCInteropKlibMetadataVersion(),
            ),
            target = target,
            output = outputPath,
            moduleName = moduleName,
            nopack = nopack,
            shortName = shortName,
            manifestProperties = manifest,
    )
}
