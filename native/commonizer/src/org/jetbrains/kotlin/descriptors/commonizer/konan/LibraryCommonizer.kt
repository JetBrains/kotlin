/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.konan.LibraryCommonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.utils.ResettableClockMark
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.Logger

internal class LibraryCommonizer internal constructor(
    private val konanDistribution: KonanDistribution,
    private val repository: Repository,
    private val dependencies: Repository,
    private val libraryLoader: NativeLibraryLoader,
    private val targets: List<KonanTarget>,
    private val resultSerializer: CommonizerResultSerializer,
    private val statsCollector: StatsCollector?,
    private val logger: Logger
) {

    private val clockMark = ResettableClockMark()

    fun run() {
        checkPreconditions()
        clockMark.reset()
        val allLibraries = loadLibraries()
        val result = commonize(allLibraries)
        resultSerializer(allLibraries, result)
        logTotal()
    }

    private fun loadLibraries(): AllNativeLibraries {
        val stdlib = libraryLoader(konanDistribution.stdlib)

        val librariesByTargets = targets.associate { target ->
            val leafTarget = LeafCommonizerTarget(target)

            val platformLibs = repository.getLibraries(leafTarget)

            if (platformLibs.isEmpty())
                logger.warning("No platform libraries found for target $target. This target will be excluded from commonization.")

            leafTarget to NativeLibrariesToCommonize(platformLibs.toList())
        }

        logProgress("Read lazy (uninitialized) libraries")

        return AllNativeLibraries(stdlib, librariesByTargets)
    }

    private fun commonize(allLibraries: AllNativeLibraries): Result {
        statsCollector.use {
            val parameters = Parameters(statsCollector, ::logProgress).apply {
                val storageManager = LockBasedStorageManager("Commonized modules")

                val stdlibProvider = NativeDistributionStdlibProvider(storageManager, allLibraries.stdlib)
                dependeeModulesProvider = stdlibProvider

                allLibraries.librariesByTargets.forEach { (target, librariesToCommonize) ->
                    if (librariesToCommonize.libraries.isEmpty()) return@forEach

                    val modulesProvider = NativeDistributionModulesProvider(storageManager, librariesToCommonize, logger)

                    val dependencyModuleProvider = NativeDistributionModulesProvider(
                        storageManager, NativeLibrariesToCommonize(dependencies.getLibraries(target).toList()), logger
                    )

                    addTarget(
                        TargetProvider(
                            target = target,
                            builtInsClass = KonanBuiltIns::class.java,
                            builtInsProvider = stdlibProvider,
                            modulesProvider = modulesProvider,
                            dependeeModulesProvider = dependencyModuleProvider // stdlib is already set as common dependency
                        )
                    )
                }
            }

            return runCommonization(parameters)
        }
    }

    private fun checkPreconditions() {
        when (targets.size) {
            0 -> logger.fatal("No targets specified")
            1 -> logger.fatal("Too few targets specified: $targets")
        }
    }

    private fun logProgress(message: String) = logger.log("* $message in ${clockMark.elapsedSinceLast()}")

    private fun logTotal() = logger.log("TOTAL: ${clockMark.elapsedSinceStart()}")
}
