/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.cli.errorAndExitJvmProcess
import org.jetbrains.kotlin.commonizer.repository.Repository
import org.jetbrains.kotlin.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.commonizer.utils.progress
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal class LibraryCommonizer internal constructor(
    private val outputTargets: Set<SharedCommonizerTarget>,
    private val repository: Repository,
    private val dependencies: Repository,
    private val resultsConsumer: ResultsConsumer,
    private val statsCollector: StatsCollector?,
    private val logger: Logger,
    private val settings: CommonizerSettings,
) {

    fun run() {
        logger.progress("Commonized all targets") {
            checkPreconditions()
            val allLibraries = loadLibraries()
            commonizeAndSaveResults(allLibraries)
        }
    }

    private fun loadLibraries(): TargetDependent<NativeLibrariesToCommonize?> {
        return logger.progress("Resolved all libraries for commonization") {
            val libraries = EagerTargetDependent(outputTargets.allLeaves()) { target ->
                repository.getLibraries(target).toList().ifNotEmpty { NativeLibrariesToCommonize(target, this) }
            }

            libraries.forEachWithTarget { target, librariesOrNull ->
                if (librariesOrNull == null)
                    logger.warning(
                        "No libraries found for target ${target}. This target will be excluded from commonization."
                    )
            }
            libraries
        }
    }

    private fun commonizeAndSaveResults(libraries: TargetDependent<NativeLibrariesToCommonize?>) {
        runCommonization(
            CommonizerParameters(
                outputTargets = outputTargets,
                targetProviders = libraries.map { target, targetLibraries -> createTargetProvider(target, targetLibraries) },
                manifestProvider = createManifestProvider(libraries),
                dependenciesProvider = createDependenciesProvider(),
                resultsConsumer = resultsConsumer,
                statsCollector = statsCollector,
                logger = logger,
                settings = settings,
            )
        )
    }

    private fun createTargetProvider(
        target: CommonizerTarget,
        libraries: NativeLibrariesToCommonize?
    ): TargetProvider? {
        if (libraries == null) return null
        return TargetProvider(
            target = target,
            modulesProvider = DefaultModulesProvider.create(libraries)
        )
    }

    private fun createDependenciesProvider(): TargetDependent<ModulesProvider?> {
        return TargetDependent(outputTargets + outputTargets.allLeaves()) { target ->
            DefaultModulesProvider.forDependencies(dependencies.getLibraries(target), logger)
        }
    }

    private fun createManifestProvider(
        libraries: TargetDependent<NativeLibrariesToCommonize?>
    ): TargetDependent<NativeManifestDataProvider> {
        return TargetDependent(outputTargets) { target ->
            when (target) {
                is LeafCommonizerTarget -> libraries[target] ?: error("Can't provide manifest for missing target $target")
                is SharedCommonizerTarget -> NativeManifestDataProvider(
                    target, target.allLeaves().mapNotNull { leafTarget -> libraries.getOrNull(leafTarget) }
                )
            }
        }
    }

    private fun checkPreconditions() {
        outputTargets.forEach { outputTarget ->
            when (outputTarget.allLeaves().size) {
                0 -> logger.errorAndExitJvmProcess("No targets specified")
                1 -> logger.errorAndExitJvmProcess("Too few targets specified: $outputTarget")
            }
        }
    }
}
