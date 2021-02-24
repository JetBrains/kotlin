/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.konan.LibraryCommonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.repository.Repository
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.utils.ProgressLogger
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal class LibraryCommonizer internal constructor(
    private val konanDistribution: KonanDistribution,
    private val repository: Repository,
    private val dependencies: Repository,
    private val libraryLoader: NativeLibraryLoader,
    private val targets: List<LeafCommonizerTarget>,
    private val resultsConsumer: ResultsConsumer,
    private val statsCollector: StatsCollector?,
    private val progressLogger: ProgressLogger
) {

    fun run() {
        checkPreconditions()
        val allLibraries = loadLibraries()
        commonizeAndSaveResults(allLibraries)
        progressLogger.logTotal()
    }

    private fun loadLibraries(): AllNativeLibraries {
        val stdlib = libraryLoader(konanDistribution.stdlib)

        val librariesByTargets = targets.associateWith { target ->
            NativeLibrariesToCommonize(repository.getLibraries(target).toList())
        }

        librariesByTargets.forEach { (target, librariesToCommonize) ->
            if (librariesToCommonize.libraries.isEmpty()) {
                progressLogger.warning("No platform libraries found for target ${target.prettyName}. This target will be excluded from commonization.")
            }
        }
        progressLogger.log("Resolved libraries to be commonized")
        return AllNativeLibraries(stdlib, librariesByTargets)
    }

    private fun commonizeAndSaveResults(allLibraries: AllNativeLibraries) {
        val manifestProvider = TargetedNativeManifestDataProvider(allLibraries)

        val parameters = CommonizerParameters(resultsConsumer, manifestProvider, statsCollector, progressLogger::log).apply {
            val storageManager = LockBasedStorageManager("Commonized modules")
            dependencyModulesProvider = NativeDistributionModulesProvider.forStandardLibrary(storageManager, allLibraries.stdlib)

            allLibraries.librariesByTargets.forEach { (target, librariesToCommonize) ->
                if (librariesToCommonize.libraries.isEmpty()) return@forEach

                val modulesProvider = NativeDistributionModulesProvider.platformLibraries(storageManager, librariesToCommonize)
                val dependencyModuleProvider = NativeDistributionModulesProvider.platformLibraries(
                    storageManager, NativeLibrariesToCommonize(dependencies.getLibraries(target).toList()),
                )

                addTarget(
                    TargetProvider(
                        target = target,
                        modulesProvider = modulesProvider,
                        dependencyModulesProvider = dependencyModuleProvider
                    )
                )
            }
        }

        runCommonization(parameters)
    }

    private fun checkPreconditions() {
        when (targets.size) {
            0 -> progressLogger.fatal("No targets specified")
            1 -> progressLogger.fatal("Too few targets specified: $targets")
        }
    }
}
