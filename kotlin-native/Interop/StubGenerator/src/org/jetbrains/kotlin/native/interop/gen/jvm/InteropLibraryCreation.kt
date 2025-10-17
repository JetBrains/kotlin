/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryLayoutForWriter
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
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
    val version = KotlinLibraryVersioning(
            abiVersion = klibAbiCompatibilityLevel.toAbiVersionForManifest(),
            compilerVersion = KotlinCompilerVersion.VERSION,
            metadataVersion = klibAbiCompatibilityLevel.toCInteropKlibMetadataVersion(),
    )
    val libFile = File(outputPath)
    val unzippedDir = if (nopack) libFile else org.jetbrains.kotlin.konan.file.createTempDir("klib")
    val layout = KonanLibraryLayoutForWriter(libFile, unzippedDir, target)
    KonanLibraryWriterImpl(
            moduleName,
            version,
            listOf(target.visibleName),
            BuiltInsPlatform.NATIVE,
            nopack = nopack,
            shortName = shortName,
            layout = layout
    ).apply {
        addMetadata(serializedMetadata)
        nativeBitcodeFiles.forEach(this::addNativeBitcode)
        addManifestAddend(manifest)
        addLinkDependencies(dependencies)
        staticLibraries.forEach(this::addIncludedBinary)
        commit()
    }
}
