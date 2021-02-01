/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.metadataVersion
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeDistributionCommonizer.StatsType.*
import org.jetbrains.kotlin.descriptors.commonizer.stats.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.ResettableClockMark
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
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
    enum class StatsType { RAW, AGGREGATED, NONE }

    private val clockMark = ResettableClockMark()

    fun run() {
        checkPreconditions()
        clockMark.reset()

        // 1. load libraries
        val allLibraries = loadLibraries()

        // 2. run commonization & write new libraries
        commonizeAndSaveResults(allLibraries)

        logTotal()
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

    private fun logProgress(message: String) = logger.log("* $message in ${clockMark.elapsedSinceLast()}")

    private fun logTotal() = logger.log("TOTAL: ${clockMark.elapsedSinceStart()}")

    private fun loadLibraries(): AllNativeLibraries {
        val stdlibPath = repository.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))
        val stdlib = NativeLibrary(loadLibrary(stdlibPath))

        val librariesByTargets = targets.associate { target ->
            val leafTarget = LeafTarget(target.name, target)

            val platformLibs = leafTarget.platformLibrariesSource
                .takeIf { it.isDirectory }
                ?.listFiles()
                ?.takeIf { it.isNotEmpty() }
                ?.map { NativeLibrary(loadLibrary(it)) }
                .orEmpty()

            if (platformLibs.isEmpty())
                logger.warning("No platform libraries found for target $target. This target will be excluded from commonization.")

            leafTarget to NativeLibrariesToCommonize(platformLibs)
        }

        logProgress("Read lazy (uninitialized) libraries")

        return AllNativeLibraries(stdlib, librariesByTargets)
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

    private fun commonizeAndSaveResults(allLibraries: AllNativeLibraries) {
        val statsCollector = when (statsType) {
            RAW -> RawStatsCollector(targets)
            AGGREGATED -> AggregatedStatsCollector(targets)
            NONE -> null
        }

        val parameters = CommonizerParameters(statsCollector, ::logProgress).apply {
            val storageManager = LockBasedStorageManager("Commonized modules")

            resultsConsumer = NativeDistributionResultsConsumer(
                repository = repository,
                originalLibraries = allLibraries,
                destination = destination,
                copyStdlib = copyStdlib,
                copyEndorsedLibs = copyEndorsedLibs,
                logProgress = ::logProgress
            )
            dependeeModulesProvider = NativeDistributionStdlibProvider(storageManager, allLibraries.stdlib)

            allLibraries.librariesByTargets.forEach { (target, librariesToCommonize) ->
                if (librariesToCommonize.libraries.isEmpty()) return@forEach

                val modulesProvider = NativeDistributionModulesProvider(storageManager, librariesToCommonize)

                addTarget(
                    TargetProvider(
                        target = target,
                        modulesProvider = modulesProvider,
                        dependeeModulesProvider = null // stdlib is already set as common dependency
                    )
                )
            }
        }

        runCommonization(parameters)

        statsCollector?.writeTo(FileStatsOutput(destination, statsType.name.toLowerCase()))
    }

    private val LeafTarget.platformLibrariesSource: File
        get() = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .resolve(name)
}
