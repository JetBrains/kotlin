/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * @property useSiteModule A target for creating Analysis API session via [analyze].
 * @property platformLibraries Platform libraries from the Kotlin Native distribution, if any.
 */
public class KaModules<Config> internal constructor(
    public val useSiteModule: KaModule,
    private val modulesToInputs: Map<KaLibraryModule, KlibInputModule<Config>>,
    public val platformLibraries: List<KaLibraryModule>,
) {
    public val inputsToModules: Map<KlibInputModule<Config>, KaLibraryModule> = modulesToInputs.map { it.value to it.key }.toMap()
    public val mainModules: List<KaLibraryModule> = modulesToInputs.keys.toList()

    public fun inputModuleFor(libraryModule: KaLibraryModule): KlibInputModule<Config>? =
        modulesToInputs[libraryModule]

    public fun configFor(module: KaLibraryModule): Config =
        inputModuleFor(module)?.config ?: error("No config for module ${module.libraryName}")
}

public fun <Config> createKaModulesForStandaloneAnalysis(
    inputs: Collection<KlibInputModule<Config>>,
    targetPlatform: TargetPlatform,
    platformLibraries: Collection<KlibInputModule<Config>> = emptyList(),
): KaModules<Config> {
    lateinit var binaryModules: Map<KaLibraryModule, KlibInputModule<Config>>
    lateinit var fakeSourceModule: KaSourceModule
    var platformLibraryModules: List<KaLibraryModule> = emptyList()
    buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = targetPlatform
            binaryModules = inputs.associateBy { inputModuleIntoKaLibraryModule(it, targetPlatform) }
            platformLibraryModules = platformLibraries.map { inputModuleIntoKaLibraryModule(it, targetPlatform) }
            // It's a pure hack: Analysis API does not properly work without root source modules.
            fakeSourceModule = addModule(
                buildKtSourceModule {
                    platform = targetPlatform
                    moduleName = "fakeSourceModule"
                    binaryModules.keys.forEach(::addRegularDependency)
                    platformLibraryModules.forEach(::addRegularDependency)
                }
            )
        }
    }
    return KaModules(fakeSourceModule, binaryModules, platformLibraryModules)
}

private fun <Config> KtModuleProviderBuilder.inputModuleIntoKaLibraryModule(
    input: KlibInputModule<Config>,
    targetPlatform: TargetPlatform,
): KaLibraryModule = addModule(
    buildKtLibraryModule {
        addBinaryRoot(input.path)
        platform = targetPlatform
        libraryName = input.name
    }
)