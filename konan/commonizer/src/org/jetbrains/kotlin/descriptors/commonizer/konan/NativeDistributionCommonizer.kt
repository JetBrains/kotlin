/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeModuleForCommonization.DeserializedModule
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeModuleForCommonization.SyntheticModule
import org.jetbrains.kotlin.descriptors.commonizer.utils.EmptyDescriptorTable
import org.jetbrains.kotlin.descriptors.commonizer.utils.ResettableClockMark
import org.jetbrains.kotlin.descriptors.commonizer.utils.createKotlinNativeForwardDeclarationsModule
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.KoltinLibraryWriterImpl
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import kotlin.time.ExperimentalTime
import org.jetbrains.kotlin.konan.file.File as KFile

class NativeDistributionCommonizer(
    private val repository: File,
    private val targets: List<KonanTarget>,
    private val destination: File,
    private val withStats: Boolean,
    private val handleError: (String) -> Nothing,
    private val log: (String) -> Unit
) {
    @ExperimentalTime
    fun run() {
        checkPreconditions()

        with(ResettableClockMark()) {
            // 1. load modules
            val modulesByTargets = loadModules()
            log("Loaded lazy (uninitialized) libraries in ${elapsedSinceLast()}")

            // 2. run commonization
            val result = commonize(modulesByTargets)
            log("Commonization performed in ${elapsedSinceLast()}")

            // 3. write new libraries
            saveModules(modulesByTargets, result)
            log("Written libraries in ${elapsedSinceLast()}")

            log("TOTAL: ${elapsedSinceStart()}")
        }

        log("Done.")
        log("")
    }

    private fun checkPreconditions() {
        if (!repository.isDirectory)
            handleError("repository does not exist: $repository")

        when (targets.size) {
            0 -> handleError("no targets specified")
            1 -> handleError("too few targets specified: $targets")
        }

        when {
            !destination.exists() -> destination.mkdirs()
            !destination.isDirectory -> handleError("output already exists: $destination")
            destination.walkTopDown().any { it != destination } -> handleError("output is not empty: $destination")
        }
    }

    private fun loadModules(): Map<InputTarget, List<NativeModuleForCommonization>> {
        val stdlibPath = repository.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))
        val stdlib = loadLibrary(stdlibPath)

        val librariesByTargets = targets.map { target ->
            val platformLibsPath = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
                .resolve(target.name)

            val platformLibs = platformLibsPath.takeIf { it.isDirectory }
                ?.listFiles()
                ?.takeIf { it.isNotEmpty() }
                ?.map { loadLibrary(it) }
                ?: handleError("no platform libraries found for target $target in $platformLibsPath")

            InputTarget(target.name, target) to platformLibs
        }.toMap()

        return librariesByTargets.mapValues { (target, libraries) ->
            val storageManager = LockBasedStorageManager("Target $target")

            val rawStdlibModule = KonanFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
                library = stdlib,
                languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                storageManager = storageManager,
                packageAccessHandler = null
            )

            val otherModules = libraries.map { library ->
                val rawModule = KonanFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                    library = library,
                    languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                    storageManager = storageManager,
                    builtIns = rawStdlibModule.builtIns,
                    packageAccessHandler = null
                )
                val data = NativeSensitiveManifestData.readFrom(library)
                DeserializedModule(rawModule, data, File(library.libraryFile.path))
            }

            val rawForwardDeclarationsModule =
                createKotlinNativeForwardDeclarationsModule(
                    storageManager = storageManager,
                    builtIns = rawStdlibModule.builtIns
                )

            val onlyDeserializedModules = listOf(rawStdlibModule) + otherModules.map { it.module }
            val allModules = onlyDeserializedModules + rawForwardDeclarationsModule
            onlyDeserializedModules.forEach { it.setDependencies(allModules) }

            val stdlibModule = DeserializedModule(
                rawStdlibModule,
                NativeSensitiveManifestData.readFrom(stdlib),
                File(stdlib.libraryFile.path)
            )
            val forwardDeclarationsModule = SyntheticModule(rawForwardDeclarationsModule)

            listOf(stdlibModule) + otherModules + forwardDeclarationsModule
        }
    }

    private fun loadLibrary(location: File): KotlinLibrary {
        if (!location.isDirectory)
            handleError("library not found: $location")

        val library = createKotlinLibrary(KFile(location.path))

        if (library.versions.metadataVersion == null)
            handleError("library does not have metadata version specified in manifest: $location")

        val metadataVersion = library.metadataVersion
        if (metadataVersion?.isCompatible() != true)
            handleError(
                """
                library has incompatible metadata version ${metadataVersion ?: "\"unknown\""}: $location,
                please make sure that all libraries passed to commonizer compatible metadata version ${KlibMetadataVersion.INSTANCE}
            """.trimIndent()
            )

        return library
    }

    private fun commonize(modulesByTargets: Map<InputTarget, List<NativeModuleForCommonization>>): CommonizationPerformed {
        val statsCollector = if (withStats) NativeStatsCollector(targets, destination) else null
        statsCollector.use {
            val parameters = CommonizationParameters(statsCollector).apply {
                modulesByTargets.forEach { (target, modules) ->
                    addTarget(target, modules.map { it.module })
                }
            }

            val result = runCommonization(parameters)
            return when (result) {
                is NothingToCommonize -> handleError("too few targets specified: ${modulesByTargets.keys}")
                is CommonizationPerformed -> result
            }
        }
    }

    private fun saveModules(
        originalModulesByTargets: Map<InputTarget, List<NativeModuleForCommonization>>,
        result: CommonizationPerformed
    ) {
        // optimization: stdlib effectively remains the same across all Kotlin/Native targets,
        // so it can be just copied to the new destination without running serializer
        val stdlibOrigin = originalModulesByTargets.values.asSequence()
            .flatten()
            .filterIsInstance<DeserializedModule>()
            .map { it.location }
            .first { it.endsWith(KONAN_STDLIB_NAME) }

        val stdlibDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(KONAN_STDLIB_NAME)
        stdlibOrigin.copyRecursively(stdlibDestination)

        val originalModulesManifestData = originalModulesByTargets.mapValues { (_, modules) ->
            modules.asSequence()
                .filterIsInstance<DeserializedModule>()
                .filter { !it.location.endsWith(KONAN_STDLIB_NAME) }
                .associate { it.module.name to it.data }
        }

        val serializer = KlibMetadataMonolithicSerializer(
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            metadataVersion = KlibMetadataVersion.INSTANCE,
            descriptorTable = EmptyDescriptorTable,
            skipExpects = false
        )

        fun serializeTarget(target: Target) {
            val libsDestination: File
            val newModulesManifestData: Map<Name, NativeSensitiveManifestData>

            when (target) {
                is InputTarget -> {
                    libsDestination = destination.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(target.konanTarget!!.name)
                    newModulesManifestData = originalModulesManifestData.getValue(target)
                }
                is OutputTarget -> {
                    libsDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
                    newModulesManifestData = originalModulesManifestData.values.first() // just take the first one
                }
            }

            val newModules = result.modulesByTargets.getValue(target)

            for (newModule in newModules) {
                val libraryName = newModule.name

                if (!shouldBeSerialized(libraryName))
                    continue

                val metadata = serializer.serializeModule(newModule)
                val manifestData = newModulesManifestData.getValue(newModule.name)
                val libraryDestination = libsDestination.resolve(libraryName.asString().trimStart('<').trimEnd('>'))

                writeLibrary(metadata, manifestData, libraryDestination)
            }
        }

        result.concreteTargets.forEach(::serializeTarget)
        result.commonTarget.let(::serializeTarget)
    }

    private fun writeLibrary(
        metadata: SerializedMetadata,
        manifestData: NativeSensitiveManifestData,
        destination: File
    ) {
        val library = KoltinLibraryWriterImpl(KFile(destination.path), manifestData.uniqueName, manifestData.versions, nopack = true)
        library.addMetadata(metadata)
        manifestData.applyTo(library.base as BaseWriterImpl)
        library.commit()
    }

    private companion object {
        val stdlibName = Name.special("<$KONAN_STDLIB_NAME>")

        fun shouldBeSerialized(libraryName: Name) =
            libraryName != stdlibName && libraryName != KlibResolvedModuleDescriptorsFactoryImpl.FORWARD_DECLARATIONS_MODULE_NAME
    }
}
