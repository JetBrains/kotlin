/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.repository.Repository
import org.jetbrains.kotlin.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.commonizer.utils.ProgressLogger
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal class LibraryCommonizer internal constructor(
    private val outputTarget: SharedCommonizerTarget,
    private val repository: Repository,
    private val dependencies: Repository,
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

    private fun loadLibraries(): TargetDependent<NativeLibrariesToCommonize?> {
        val libraries = EagerTargetDependent(outputTarget.allLeaves()) { target ->
            repository.getLibraries(target).toList().ifNotEmpty(::NativeLibrariesToCommonize)
        }

        libraries.forEachWithTarget { target, librariesOrNull ->
            if (librariesOrNull == null)
                progressLogger.warning(
                    "No libraries found for target ${target.prettyName}. This target will be excluded from commonization."
                )
        }

        progressLogger.log("Resolved libraries to be commonized")
        return libraries
    }

    private fun commonizeAndSaveResults(libraries: TargetDependent<NativeLibrariesToCommonize?>) {
        val parameters = CommonizerParameters(
            outputTarget = outputTarget,
            targetProviders = libraries.map { target, targetLibraries -> createTargetProvider(target, targetLibraries) },
            manifestProvider = createManifestProvider(libraries),
            dependenciesProvider = createDependenciesProvider(),
            resultsConsumer = resultsConsumer,
            statsCollector = statsCollector,
            progressLogger = progressLogger::log
        )
        runCommonization(parameters)
    }

    private fun createTargetProvider(target: CommonizerTarget, libraries: NativeLibrariesToCommonize?): TargetProvider? {
        if (libraries == null) return null
        return TargetProvider(
            target = target,
            modulesProvider = DefaultModulesProvider.create(libraries)
        )
    }

    private fun createDependenciesProvider(): TargetDependent<ModulesProvider?> {
        return TargetDependent(outputTarget.withAllAncestors()) { target ->
            DefaultModulesProvider.create(dependencies.getLibraries(target))
        }
    }

    private fun createManifestProvider(
        libraries: TargetDependent<NativeLibrariesToCommonize?>
    ): TargetDependent<NativeManifestDataProvider> {
        return TargetDependent(outputTarget.withAllAncestors()) { target ->
            when (target) {
                is LeafCommonizerTarget -> libraries[target] ?: error("Can't provide manifest for missing target $target")
                is SharedCommonizerTarget -> CommonNativeManifestDataProvider(
                    target.allLeaves().mapNotNull { leafTarget -> libraries.getOrNull(leafTarget) }
                )
            }
        }
    }

    private fun checkPreconditions() {
        when (outputTarget.allLeaves().size) {
            0 -> progressLogger.fatal("No targets specified")
            1 -> progressLogger.fatal("Too few targets specified: $outputTarget")
        }
    }
}
