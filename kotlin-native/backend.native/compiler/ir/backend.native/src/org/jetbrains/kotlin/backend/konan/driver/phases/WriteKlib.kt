/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.common.serialization.addLanguageFeaturesToManifest
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.LightPhaseContext
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import java.util.*

internal data class KlibWriterInput(
        val serializerOutput: SerializerOutput<KonanLibrary>,
        val customOutputPath: String?,
        val produceHeaderKlib: Boolean,
)

internal val WriteKlibPhase = createSimpleNamedCompilerPhase<LightPhaseContext, KlibWriterInput>(
        "WriteKlib",
) { context, input ->
    val config = context.config
    val configuration = config.configuration
    val outputFiles = OutputFiles(input.customOutputPath
            ?: config.outputPath, config.target, config.produce)
    val nopack = configuration.getBoolean(KonanConfigKeys.NOPACK)
    val output = outputFiles.klibOutputFileName(!nopack)
    val libraryName = config.moduleId
    val shortLibraryName = config.shortModuleName
    val versions = KotlinLibraryVersioning(
        compilerVersion = KotlinCompilerVersion.getVersion().toString(),
        abiVersion = configuration.klibAbiVersionForManifest(),
        metadataVersion = configuration.klibMetadataVersionOrDefault(),
    )
    val target = config.target
    val manifestProperties = config.manifestProperties ?: Properties()

    if (input.produceHeaderKlib) {
        manifestProperties.setProperty(KLIB_PROPERTY_HEADER, "true")
    }

    addLanguageFeaturesToManifest(manifestProperties, configuration.languageVersionSettings)

    val nativeTargetsForManifest = config.nativeTargetsForManifest?.map { it.visibleName } ?: listOf(target.visibleName)

    if (!nopack) {
        val suffix = outputFiles.produce.suffix(target)
        if (!output.endsWith(suffix)) {
            error("please specify correct output: packed: ${!nopack}, $output$suffix")
        }
    }

    /*
    metadata libraries do not have 'link' dependencies, as there are several reasons
    why a consumer might not be able to provide the same compile classpath as the producer
    (e.g. commonized cinterops, host vs client environment differences).
    */
    val linkDependencies = if (context.config.metadataKlib) emptyList()
    else input.serializerOutput.neededLibraries

    config.writeDependenciesOfProducedKlibTo?.let { path ->
        val usedDependenciesFile = File(path)
        // We write out the absolute path instead of canonical here to avoid resolving symbolic links
        // as that can make it difficult to map the dependencies back to the command line arguments.
        usedDependenciesFile.writeLines(linkDependencies.map { it.libraryFile.absolutePath })
    }

    buildLibrary(
            natives = config.nativeLibraries,
            included = config.includeBinaries,
            linkDependencies = linkDependencies,
            metadata = input.serializerOutput.serializedMetadata!!,
            ir = input.serializerOutput.serializedIr,
            versions = versions,
            target = target,
            nativeTargetsForManifest = nativeTargetsForManifest,
            output = output,
            moduleName = libraryName,
            nopack = nopack,
            shortName = shortLibraryName,
            manifestProperties = manifestProperties,
    )
}

internal fun <T : LightPhaseContext> PhaseEngine<T>.writeKlib(
        serializationOutput: SerializerOutput<KonanLibrary>,
        customOutputPath: String? = null,
        produceHeaderKlib: Boolean = false,
) {
    this.runPhase(WriteKlibPhase, KlibWriterInput(serializationOutput, customOutputPath, produceHeaderKlib))
}
