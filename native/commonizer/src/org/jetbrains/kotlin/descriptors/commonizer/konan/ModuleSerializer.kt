/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import org.jetbrains.kotlin.util.Logger
import java.io.File

internal class ModuleSerializer(
    private val destination: File,
    private val outputLayout: CommonizerOutputLayout,
) : ResultsConsumer {
    override fun consume(target: CommonizerTarget, moduleResult: ResultsConsumer.ModuleResult) {
        val librariesDestination = outputLayout.getTargetDirectory(destination, target)
        when (moduleResult) {
            is ResultsConsumer.ModuleResult.Commonized -> {
                val libraryName = moduleResult.libraryName
                val libraryDestination = librariesDestination.resolve(libraryName)
                writeLibrary(moduleResult.metadata, moduleResult.manifest, libraryDestination)
            }
            is ResultsConsumer.ModuleResult.Missing -> {
                val libraryName = moduleResult.libraryName
                val missingModuleLocation = moduleResult.originalLocation
                missingModuleLocation.copyRecursively(librariesDestination.resolve(libraryName))
            }
        }
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
        nativeTargets = emptyList(), // will be overwritten with NativeSensitiveManifestData.applyTo() below
        nopack = true,
        shortName = manifestData.shortName,
        layout = layout
    )
    library.addMetadata(metadata)
    manifestData.applyTo(library.base as BaseWriterImpl)
    library.commit()
}
