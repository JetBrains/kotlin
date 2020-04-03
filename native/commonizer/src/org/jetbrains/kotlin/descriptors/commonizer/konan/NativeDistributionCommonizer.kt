/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.utils.ResettableClockMark
import org.jetbrains.kotlin.descriptors.konan.NATIVE_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KoltinLibraryWriterImpl
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.Logger
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

class NativeDistributionCommonizer(
    private val repository: File,
    private val targets: List<KonanTarget>,
    private val destination: File,
    private val copyStdlib: Boolean,
    private val copyEndorsedLibs: Boolean,
    private val withStats: Boolean,
    private val logger: Logger
) {
    fun run() {
        checkPreconditions()

        with(ResettableClockMark()) {
            // 1. load libraries
            val librariesByTargets = loadLibraries()
            logger.log("* Loaded lazy (uninitialized) libraries in ${elapsedSinceLast()}")

            // 2. run commonization
            val result = commonize(librariesByTargets)
            logger.log("* Commonization performed in ${elapsedSinceLast()}")

            // 3. write new libraries
            saveModules(librariesByTargets, result)
            logger.log("* Written libraries in ${elapsedSinceLast()}")

            logger.log("TOTAL: ${elapsedSinceStart()}")
        }
    }

    private fun checkPreconditions() {
        if (!repository.isDirectory)
            logger.fatal("repository does not exist: $repository")

        when (targets.size) {
            0 -> logger.fatal("no targets specified")
            1 -> logger.fatal("too few targets specified: $targets")
        }

        when {
            !destination.exists() -> destination.mkdirs()
            !destination.isDirectory -> logger.fatal("output already exists: $destination")
            destination.walkTopDown().any { it != destination } -> logger.fatal("output is not empty: $destination")
        }
    }

    private fun loadLibraries(): Map<InputTarget, NativeDistributionLibraries> {
        val stdlibPath = repository.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))
        val stdlib = loadLibrary(stdlibPath)

        return targets.associate { target ->
            val platformLibsPath = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
                .resolve(target.name)

            val platformLibs = platformLibsPath.takeIf { it.isDirectory }
                ?.listFiles()
                ?.takeIf { it.isNotEmpty() }
                ?.map { loadLibrary(it) }
                ?: logger.fatal("no platform libraries found for target $target in $platformLibsPath")

            InputTarget(target.name, target) to NativeDistributionLibraries(stdlib, platformLibs)
        }
    }

    private fun loadLibrary(location: File): KotlinLibrary {
        if (!location.isDirectory)
            logger.fatal("library not found: $location")

        val library = resolveSingleFileKlib(
            libraryFile = KFile(location.path),
            logger = logger,
            strategy = ToolingSingleFileKlibResolveStrategy
        )

        if (library.versions.metadataVersion == null)
            logger.fatal("library does not have metadata version specified in manifest: $location")

        val metadataVersion = library.metadataVersion
        if (metadataVersion?.isCompatible() != true)
            logger.fatal(
                """
                library has incompatible metadata version ${metadataVersion ?: "\"unknown\""}: $location,
                please make sure that all libraries passed to commonizer compatible metadata version ${KlibMetadataVersion.INSTANCE}
                """.trimIndent()
            )

        return library
    }

    private fun commonize(librariesByTargets: Map<InputTarget, NativeDistributionLibraries>): CommonizationPerformed {
        val statsCollector = if (withStats) NativeStatsCollector(targets, destination) else null
        statsCollector.use {
            val parameters = Parameters(statsCollector).apply {
                librariesByTargets.forEach { (target, libraries) ->
                    val provider = NativeDistributionModulesProvider(
                        storageManager = LockBasedStorageManager("Target $target"),
                        libraries = libraries
                    )
                    addTarget(
                        TargetProvider(
                            target = target,
                            builtInsClass = KonanBuiltIns::class.java,
                            builtInsProvider = provider,
                            modulesProvider = provider
                        )
                    )
                }
            }

            val result = runCommonization(parameters)
            return when (result) {
                is NothingToCommonize -> logger.fatal("too few targets specified: ${librariesByTargets.keys}")
                is CommonizationPerformed -> result
            }
        }
    }

    private fun saveModules(
        originalLibrariesByTargets: Map<InputTarget, NativeDistributionLibraries>,
        result: CommonizationPerformed
    ) {
        // optimization: stdlib and endorsed libraries effectively remain the same across all Kotlin/Native targets,
        // so they can be just copied to the new destination without running serializer
        if (copyStdlib || copyEndorsedLibs) {
            repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
                .listFiles()
                ?.filter { it.isDirectory }
                ?.let {
                    if (copyStdlib) {
                        if (copyEndorsedLibs) it else it.filter { dir -> dir.endsWith(KONAN_STDLIB_NAME) }
                    } else
                        it.filter { dir -> !dir.endsWith(KONAN_STDLIB_NAME) }
                }?.forEach { libraryOrigin ->
                    val libraryDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(libraryOrigin.name)
                    libraryOrigin.copyRecursively(libraryDestination)
                }
        }

        val commonManifestProvider = CommonNativeManifestDataProvider(originalLibrariesByTargets.values)

        val serializer = KlibMetadataMonolithicSerializer(
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            metadataVersion = KlibMetadataVersion.INSTANCE,
            skipExpects = false
        )

        fun serializeTarget(target: Target) {
            val libsDestination: File
            val manifestProvider: NativeManifestDataProvider

            when (target) {
                is InputTarget -> {
                    libsDestination = destination.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(target.konanTarget!!.name)
                    manifestProvider = originalLibrariesByTargets.getValue(target)
                }
                is OutputTarget -> {
                    libsDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
                    manifestProvider = commonManifestProvider
                }
            }

            val newModules = result.modulesByTargets.getValue(target)

            for (newModule in newModules) {
                val libraryName = newModule.name

                if (!shouldBeSerialized(libraryName))
                    continue

                val metadata = serializer.serializeModule(newModule)
                val plainName = libraryName.asString().removePrefix("<").removeSuffix(">")

                val manifestData = manifestProvider.getManifest(plainName)
                val libraryDestination = libsDestination.resolve(plainName)

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
        val library = KoltinLibraryWriterImpl(
            libDir = KFile(destination.path),
            moduleName = manifestData.uniqueName,
            versions = manifestData.versions,
            builtInsPlatform = BuiltInsPlatform.NATIVE,
            nativeTargets = emptyList(), // will be overwritten with NativeSensitiveManifestData.applyTo() below
            nopack = true,
            shortName = manifestData.shortName
        )
        library.addMetadata(metadata)
        manifestData.applyTo(library.base as BaseWriterImpl)
        library.commit()
    }

    private companion object {
        fun shouldBeSerialized(libraryName: Name) =
            libraryName != NATIVE_STDLIB_MODULE_NAME && libraryName != KlibResolvedModuleDescriptorsFactoryImpl.FORWARD_DECLARATIONS_MODULE_NAME
    }
}
