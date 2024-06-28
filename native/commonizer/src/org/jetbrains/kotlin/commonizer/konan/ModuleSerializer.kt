/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import java.io.File

internal class ModuleSerializer(
    private val destination: File,
) : ResultsConsumer {
    override fun consume(parameters: CommonizerParameters, target: CommonizerTarget, moduleResult: ResultsConsumer.ModuleResult) {
        val librariesDestination = CommonizerOutputFileLayout.resolveCommonizedDirectory(
            destination,
            target,
        )
        writeLibrary(
            moduleResult.metadata,
            moduleResult.manifest,
            librariesDestination.resolve(moduleResult.fileSystemCompatibleLibraryName)
        )
    }
}

private fun writeLibrary(
    metadata: SerializedMetadata,
    manifestData: NativeSensitiveManifestData,
    libraryDestination: File
) {
    val layout = org.jetbrains.kotlin.konan.file.File(libraryDestination.path).let { KotlinLibraryLayoutForWriter(it, it) }
    val library = KotlinLibraryWriterImpl(
        moduleName = manifestData.uniqueName,
        versions = manifestData.versions,
        builtInsPlatform = BuiltInsPlatform.NATIVE,
        nativeTargets = emptyList(), // will be overwritten with addManifest(manifestData) below
        nopack = true,
        shortName = manifestData.shortName,
        layout = layout
    )
    library.addMetadata(metadata)
    (library.base as BaseWriterImpl).addManifest(manifestData)
    library.commit()
}

/**
 * Returns [ResultsConsumer.ModuleResult.libraryName] but replaces potentially occurring colons ':' with underscores '_'.
 * Colons cannot be used as filenames on Windows, but they occured as libraryNames for Klibs produced by the cinterop tool.
 */
val ResultsConsumer.ModuleResult.fileSystemCompatibleLibraryName: String
    get() = libraryName.replace(":", "_")
