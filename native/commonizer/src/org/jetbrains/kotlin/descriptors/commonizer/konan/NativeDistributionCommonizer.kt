/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.Target
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeDistributionCommonizer.StatsType.*
import org.jetbrains.kotlin.descriptors.commonizer.stats.AggregatedStatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.stats.FileStatsOutput
import org.jetbrains.kotlin.descriptors.commonizer.stats.RawStatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.utils.ResettableClockMark
import org.jetbrains.kotlin.descriptors.konan.NATIVE_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
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
    private val statsType: StatsType,
    private val logger: Logger
) {
    enum class StatsType {
        RAW, AGGREGATED, NONE
    }

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
            logger.fatal("Repository does not exist: $repository")

        when (targets.size) {
            0 -> logger.fatal("No targets specified")
            1 -> logger.fatal("Too few targets specified: $targets")
        }

        when {
            !destination.exists() -> destination.mkdirs()
            !destination.isDirectory -> logger.fatal("Output already exists: $destination")
            destination.walkTopDown().any { it != destination } -> logger.fatal("Output is not empty: $destination")
        }
    }

    private fun loadLibraries(): Map<InputTarget, NativeDistributionLibraries> {
        val stdlibPath = repository.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))
        val stdlib = loadLibrary(stdlibPath)

        return targets.associate { target ->
            val inputTarget = InputTarget(target.name, target)

            val platformLibs = inputTarget.platformLibrariesSource
                .takeIf { it.isDirectory }
                ?.listFiles()
                ?.takeIf { it.isNotEmpty() }
                ?.map { loadLibrary(it) }
                .orEmpty()

            if (platformLibs.isEmpty())
                logger.warning("No platform libraries found for target $target. This target will be excluded from commonization.")

            inputTarget to NativeDistributionLibraries(stdlib, platformLibs)
        }
    }

    private fun loadLibrary(location: File): KotlinLibrary {
        if (!location.isDirectory)
            logger.fatal("Library not found: $location")

        val library = resolveSingleFileKlib(
            libraryFile = KFile(location.path),
            logger = logger,
            strategy = ToolingSingleFileKlibResolveStrategy
        )

        if (library.versions.metadataVersion == null)
            logger.fatal("Library does not have metadata version specified in manifest: $location")

        val metadataVersion = library.metadataVersion
        if (metadataVersion?.isCompatible() != true)
            logger.fatal(
                """
                Library has incompatible metadata version ${metadataVersion ?: "\"unknown\""}: $location
                Please make sure that all libraries passed to commonizer compatible metadata version ${KlibMetadataVersion.INSTANCE}
                """.trimIndent()
            )

        return library
    }

    private fun commonize(librariesByTargets: Map<InputTarget, NativeDistributionLibraries>): Result {
        val statsCollector = when (statsType) {
            RAW -> RawStatsCollector(targets, FileStatsOutput(destination, "raw"))
            AGGREGATED -> AggregatedStatsCollector(targets, FileStatsOutput(destination, "aggregated"))
            NONE -> null
        }
        statsCollector.use {
            val parameters = Parameters(statsCollector).apply {
                librariesByTargets.forEach { (target, libraries) ->
                    if (libraries.platformLibs.isEmpty()) return@forEach

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

            return runCommonization(parameters)
        }
    }

    private fun saveModules(
        originalLibrariesByTargets: Map<InputTarget, NativeDistributionLibraries>,
        result: Result
    ) {
        // optimization: stdlib and endorsed libraries effectively remain the same across all Kotlin/Native targets,
        // so they can be just copied to the new destination without running serializer
        copyCommonStandardLibraries()

        when (result) {
            is NothingToCommonize -> {
                // It may happen that all targets to be commonized (or at least all but one target) miss platform libraries.
                // In such case commonizer will do nothing and return a special result value 'NothingToCommonize'.
                // So, let's just copy platform libraries from those target where they are to the new destination.
                originalLibrariesByTargets.keys.forEach(::copyTargetAsIs)
            }

            is CommonizationPerformed -> {
                val serializer = KlibMetadataMonolithicSerializer(
                    languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                    metadataVersion = KlibMetadataVersion.INSTANCE,
                    skipExpects = false
                )

                // 'targetsToCopy' are some targets with empty set of platform libraries
                val targetsToCopy = originalLibrariesByTargets.keys - result.concreteTargets
                targetsToCopy.forEach(::copyTargetAsIs)

                val targetsToSerialize = result.concreteTargets + result.commonTarget
                targetsToSerialize.forEach { target ->
                    val newModules = result.modulesByTargets.getValue(target)

                    val manifestProvider = when (target) {
                        is InputTarget -> originalLibrariesByTargets.getValue(target)
                        is OutputTarget -> CommonNativeManifestDataProvider(originalLibrariesByTargets.values)
                    }

                    serializeTarget(target, newModules, manifestProvider, serializer)
                }
            }
        }
    }

    private fun copyCommonStandardLibraries() {
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
    }

    private fun copyTargetAsIs(target: InputTarget) {
        val librariesDestination = target.librariesDestination
        librariesDestination.mkdirs() // always create an empty directory even if there is nothing to copy

        val librariesSource = target.platformLibrariesSource
        if (librariesSource.isDirectory) librariesSource.copyRecursively(librariesDestination)
    }

    private fun serializeTarget(
        target: Target,
        newModules: Collection<ModuleDescriptor>,
        manifestProvider: NativeManifestDataProvider,
        serializer: KlibMetadataMonolithicSerializer
    ) {
        val librariesDestination = target.librariesDestination

        for (newModule in newModules) {
            val libraryName = newModule.name

            if (!shouldBeSerialized(libraryName))
                continue

            val metadata = serializer.serializeModule(newModule)
            val plainName = libraryName.asString().removePrefix("<").removeSuffix(">")

            val manifestData = manifestProvider.getManifest(plainName)
            val libraryDestination = librariesDestination.resolve(plainName)

            writeLibrary(metadata, manifestData, libraryDestination)
        }
    }

    private fun writeLibrary(
        metadata: SerializedMetadata,
        manifestData: NativeSensitiveManifestData,
        destination: File
    ) {
        val library = KotlinLibraryWriterImpl(
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

    private val InputTarget.platformLibrariesSource: File
        get() = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .resolve(name)

    private val Target.librariesDestination: File
        get() = when (this) {
            is InputTarget -> destination.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(name)
            is OutputTarget -> destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
        }

    private companion object {
        fun shouldBeSerialized(libraryName: Name) =
            libraryName != NATIVE_STDLIB_MODULE_NAME && libraryName != KlibResolvedModuleDescriptorsFactoryImpl.FORWARD_DECLARATIONS_MODULE_NAME
    }
}
