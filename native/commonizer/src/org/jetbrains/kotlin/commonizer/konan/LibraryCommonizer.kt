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
        return librariesByTargets
    }

    private fun commonizeAndSaveResults(allLibraries: TargetDependent<NativeLibrariesToCommonize>) {
        val parameters = CommonizerParameters(
            resultsConsumer = resultsConsumer,
            manifestDataProvider = TargetedNativeManifestDataProvider(allLibraries),
            commonDependencyModulesProvider = DefaultModulesProvider.create(dependencies.getLibraries(commonTarget)),
            statsCollector = statsCollector,
            progressLogger = progressLogger::log
        )

        allLibraries.forEach { (target, librariesToCommonize) ->
            parameters.addTarget(target, librariesToCommonize)
        }

        runCommonization(parameters)
    }

    private fun CommonizerParameters.addTarget(target: CommonizerTarget, libraries: NativeLibrariesToCommonize) {
        if (libraries.libraries.isEmpty()) return

        val modulesProvider = DefaultModulesProvider.create(libraries)
        val dependencyModuleProvider = DefaultModulesProvider.create(dependencies.getLibraries(target))

        addTarget(
            TargetProvider(
                target = target,
                modulesProvider = modulesProvider,
                dependencyModulesProvider = dependencyModuleProvider
            )
        )
    }

    private fun checkPreconditions() {
        when (commonTarget.targets.size) {
            0 -> progressLogger.fatal("No targets specified")
            1 -> progressLogger.fatal("Too few targets specified: $commonTarget")
        }
    }
}
