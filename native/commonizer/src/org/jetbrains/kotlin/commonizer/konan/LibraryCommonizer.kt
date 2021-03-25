/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.repository.Repository
import org.jetbrains.kotlin.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.commonizer.utils.ProgressLogger

internal class LibraryCommonizer internal constructor(
    private val repository: Repository,
    private val dependencies: Repository,
    private val commonTarget: SharedCommonizerTarget,
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

    private fun loadLibraries(): TargetDependent<NativeLibrariesToCommonize> {
        val librariesByTargets = commonTarget.targets.associateWith { target ->
            NativeLibrariesToCommonize(repository.getLibraries(target).toList())
        }

        librariesByTargets.forEach { (target, librariesToCommonize) ->
            if (librariesToCommonize.libraries.isEmpty()) {
                progressLogger.warning("No platform libraries found for target ${target.prettyName}. This target will be excluded from commonization.")
            }
        }
        progressLogger.log("Resolved libraries to be commonized")
        return TargetDependent(librariesByTargets)
    }

    private fun commonizeAndSaveResults(allLibraries: TargetDependent<NativeLibrariesToCommonize>) {
        val parameters = CommonizerParameters(
            targetProviders = TargetDependent(commonTarget.targets) { target -> createTargetProvider(target, allLibraries[target]) }
                .filterNonNull(),
            resultsConsumer = resultsConsumer,
            commonManifestProvider = CommonNativeManifestDataProvider(commonTarget.targets.map { allLibraries[it] }),
            commonDependencyModulesProvider = DefaultModulesProvider.create(dependencies.getLibraries(commonTarget)),
            statsCollector = statsCollector,
            progressLogger = progressLogger::log
        )
        runCommonization(parameters)
    }

    private fun createTargetProvider(target: CommonizerTarget, libraries: NativeLibrariesToCommonize): TargetProvider? {
        if (libraries.libraries.isEmpty()) return null
        return TargetProvider(
            target = target,
            modulesProvider = DefaultModulesProvider.create(libraries),
            dependencyModulesProvider = DefaultModulesProvider.create(dependencies.getLibraries(target)),
            manifestProvider = libraries,
        )
    }

    private fun checkPreconditions() {
        when (commonTarget.targets.size) {
            0 -> progressLogger.fatal("No targets specified")
            1 -> progressLogger.fatal("Too few targets specified: $commonTarget")
        }
    }
}
