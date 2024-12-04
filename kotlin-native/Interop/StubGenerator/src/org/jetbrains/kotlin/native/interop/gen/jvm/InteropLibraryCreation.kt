/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import kotlinx.metadata.klib.*
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryLayoutForWriter
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import java.util.*

fun createInteropLibrary(
    metadata: KlibModuleMetadata,
    outputPath: String,
    moduleName: String,
    nativeBitcodeFiles: List<String>,
    target: KonanTarget,
    manifest: Properties,
    dependencies: List<KotlinLibrary>,
    nopack: Boolean,
    shortName: String?,
    staticLibraries: List<String>
) {
    val version = KotlinLibraryVersioning(
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KotlinCompilerVersion.VERSION,
            metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
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
        val serializedMetadata = metadata.write(ChunkedKlibModuleFragmentWriteStrategy(topLevelClassifierDeclarationsPerFile = 128))
        addMetadata(SerializedMetadata(serializedMetadata.header, serializedMetadata.fragments, serializedMetadata.fragmentNames))
        nativeBitcodeFiles.forEach(this::addNativeBitcode)
        addManifestAddend(manifest)
        addLinkDependencies(dependencies)
        staticLibraries.forEach(this::addIncludedBinary)
        commit()
    }
}
