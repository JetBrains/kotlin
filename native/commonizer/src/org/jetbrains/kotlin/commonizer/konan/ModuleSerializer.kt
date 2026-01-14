/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.commonizer.CommonizerParameters
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.ResultsConsumer
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.asComponentWriter
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
    KlibWriter {
        manifest {
            moduleName(manifestData.uniqueName)
            versions(manifestData.versions)
            platformAndTargets(BuiltInsPlatform.NATIVE, manifestData.nativeTargets)
            customProperties { addNativeSensitiveManifestProperties(manifestData) }
        }
        include(metadata.asComponentWriter())
    }.writeTo(libraryDestination.absolutePath)
}

/**
 * Returns [ResultsConsumer.ModuleResult.libraryName] but replaces potentially occurring colons ':' with underscores '_'.
 * Colons cannot be used as filenames on Windows, but they occured as libraryNames for Klibs produced by the cinterop tool.
 */
val ResultsConsumer.ModuleResult.fileSystemCompatibleLibraryName: String
    get() = libraryName.replace(":", "_")
