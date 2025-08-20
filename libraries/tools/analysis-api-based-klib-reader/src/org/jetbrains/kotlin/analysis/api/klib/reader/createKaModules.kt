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
import org.jetbrains.kotlin.platform.TargetPlatform
import java.nio.file.Path

/**
 * @property useSiteModule A target for creating Analysis API session via [analyze].
 * @property platformLibraries Platform libraries from the Kotlin Native distribution, if any.
 */
public class KaModules<InputModule> internal constructor(
    public val useSiteModule: KaModule,
    private val modulesToInputs: Map<KaLibraryModule, InputModule>,
    public val platformLibraries: List<KaLibraryModule>,
) {
    public val inputsToModules: Map<InputModule, KaLibraryModule> = modulesToInputs.map { it.value to it.key }.toMap()
    public val mainModules: List<KaLibraryModule> = modulesToInputs.keys.toList()

    public fun inputModuleFor(libraryModule: KaLibraryModule): InputModule? =
        modulesToInputs[libraryModule]
}

public abstract class KaModulesFactory<InputModule> {
    protected abstract val InputModule.moduleName: String
    protected abstract val InputModule.modulePath: Path

    public fun createKaModulesForStandaloneAnalysis(
        inputs: Collection<InputModule>,
        targetPlatform: TargetPlatform,
        platformLibraries: Collection<InputModule> = emptyList(),
    ): KaModules<InputModule> {
        lateinit var binaryModules: Map<KaLibraryModule, InputModule>
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

    private fun KtModuleProviderBuilder.inputModuleIntoKaLibraryModule(
        input: InputModule,
        targetPlatform: TargetPlatform,
    ): KaLibraryModule = addModule(
        buildKtLibraryModule {
            addBinaryRoot(input.modulePath)
            platform = targetPlatform
            libraryName = input.moduleName
        }
    )
}
