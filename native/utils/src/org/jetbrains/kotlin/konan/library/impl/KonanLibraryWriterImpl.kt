/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.BitcodeWriter
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.impl.*

class KonanLibraryLayoutForWriter(
    libFile: File,
    unzippedDir: File,
    override val target: KonanTarget,
) : KonanLibraryLayout, KotlinLibraryLayoutForWriter(libFile, unzippedDir)

/**
 * Requires non-null [target].
 */
private class KonanLibraryWriterImpl(
    moduleName: String,
    versions: KotlinLibraryVersioning,
    nativeTargets: List<String>,
    builtInsPlatform: BuiltInsPlatform,
    nopack: Boolean = false,
    shortName: String? = null,
    val layout: KonanLibraryLayoutForWriter,
    base: BaseWriter = BaseWriterImpl(layout, moduleName, versions, builtInsPlatform, nativeTargets, nopack, shortName),
    bitcode: BitcodeWriter = BitcodeWriterImpl(layout),
) : BaseWriter by base, BitcodeWriter by bitcode, KonanLibraryWriter

fun buildLibrary(
    natives: List<String>,
    included: List<String>,
    linkDependencies: List<KotlinLibrary>,
    metadata: SerializedMetadata,
    ir: SerializedIrModule?,
    versions: KotlinLibraryVersioning,
    target: KonanTarget,
    output: String,
    moduleName: String,
    nopack: Boolean,
    shortName: String?,
    manifestProperties: Properties?,
    /**
     * This property affects *only* the property of 'native_targets' written in manifest
     */
    nativeTargetsForManifest: List<String> = listOf(target.visibleName),
): KonanLibraryLayout {

    val libFile = File(output)
    val unzippedDir = if (nopack) libFile else org.jetbrains.kotlin.konan.file.createTempDir("klib")

    val layout = KonanLibraryLayoutForWriter(libFile, unzippedDir, target)
    val library = KonanLibraryWriterImpl(
        moduleName,
        versions,
        nativeTargetsForManifest,
        BuiltInsPlatform.NATIVE,
        nopack,
        shortName,
        layout
    )

    if (versions.abiVersion?.major == 1 && versions.abiVersion?.minor == 201) {
        File(layout.targetDir, "kotlin").mkdirs()
    }

    natives.forEach {
        library.addNativeBitcode(it)
    }
    included.forEach {
        library.addIncludedBinary(it)
    }
    manifestProperties?.let { library.addManifestAddend(it) }
    library.addLinkDependencies(linkDependencies)

    val metadataWriter = KlibMetadataWriterImpl(KlibMetadataComponentLayout(unzippedDir))
    metadataWriter.writeMetadata(metadata)

    if (ir != null) {
        val mainIrWriter = KlibIrWriterImpl.ForMainIr(KlibIrComponentLayout.createForMainIr(unzippedDir))
        mainIrWriter.writeMainIr(ir.files)
    }

    ir?.fileWithPreparedInlinableFunctions?.let { inlinableFunctionsFile ->
        val inlinableFunctionsWriter = KlibIrWriterImpl.ForInlinableFunctionsIr(
            layout = KlibIrComponentLayout.createForInlinableFunctionsIr(unzippedDir)
        )
        inlinableFunctionsWriter.writeInlinableFunctionsIr(inlinableFunctionsFile)
    }

    library.commit()

    return library.layout
}
