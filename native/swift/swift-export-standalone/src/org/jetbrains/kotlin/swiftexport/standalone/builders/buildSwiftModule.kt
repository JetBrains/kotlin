/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.native.analysis.api.KlibScope
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.io.path.Path

internal fun buildSwiftModule(
    input: InputModule,
    kotlinDistribution: Distribution,
    shouldSortInputFiles: Boolean,
    bridgeModuleName: String,
): SirModule {

    val (module, scopeProvider) = when (input) {
        is InputModule.BinaryModule -> constructLibraryModule(kotlinDistribution, input)
        is InputModule.SourceModule -> constructSourceModule(kotlinDistribution, input, shouldSortInputFiles)
    }

    return analyze(module) {
        with(StandaloneSirSession(this, bridgeModuleName)) {
            val result = module.sirModule()
            scopeProvider(this@analyze).flatMap {
                it.extractDeclarations()
            }
            result
        }
    }
}

internal data class ModuleWithScopeProvider(
    val ktModule: KtModule,
    val scopeProvider: (KtAnalysisSession) -> List<KtScope>
)

@OptIn(KtAnalysisApiInternals::class)
private fun constructAnalysisAPISession(
    kotlinDistribution: Distribution,
    moduleBuilder: KtModuleProviderBuilder.(stdlibModule: KtLibraryModule) -> KtModule,
): StandaloneAnalysisAPISession {
    val analysisAPISession = buildStandaloneAnalysisAPISession {
        registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())

        buildKtModuleProvider {
            platform = NativePlatforms.unspecifiedNativePlatform
            val stdlib = addModule(
                buildKtLibraryModule {
                    addBinaryRoot(Path(kotlinDistribution.stdlib))
                    platform = NativePlatforms.unspecifiedNativePlatform
                    libraryName = "stdlib"
                }
            )
            addModule(moduleBuilder(stdlib))
        }
    }
    return analysisAPISession
}

private fun constructLibraryModule(
    kotlinDistribution: Distribution,
    input: InputModule.BinaryModule,
): ModuleWithScopeProvider {
    lateinit var module: KtLibraryModule
    lateinit var fakeSrcModule: KtSourceModule
    constructAnalysisAPISession(kotlinDistribution) { stdlibModule ->
        module = buildKtLibraryModule {
            addBinaryRoot(input.path)
            platform = NativePlatforms.unspecifiedNativePlatform
            libraryName = input.name
            addRegularDependency(stdlibModule)
        }
        fakeSrcModule = buildKtSourceModule {
            val tempFile = kotlin.io.path.createTempFile("fake", ".kt")
            addSourceRoot(tempFile)
            platform = NativePlatforms.unspecifiedNativePlatform
            moduleName = "fake"
            addRegularDependency(module)
            addRegularDependency(stdlibModule)
        }
        fakeSrcModule
    }
    return ModuleWithScopeProvider(fakeSrcModule) { ktAnalysisSession ->
        listOf(KlibScope(module, ktAnalysisSession))
    }
}

private fun constructSourceModule(
    kotlinDistribution: Distribution,
    input: InputModule.SourceModule,
    shouldSortInputFiles: Boolean,
): ModuleWithScopeProvider {
    val analysisAPISession = constructAnalysisAPISession(kotlinDistribution) { stdlibModule ->
        buildKtSourceModule {
            addSourceRoot(input.path)
            platform = NativePlatforms.unspecifiedNativePlatform
            moduleName = input.name
            addRegularDependency(stdlibModule)
        }
    }
    val (sourceModule, rawFiles) = analysisAPISession.modulesWithFiles.entries.single()
    val ktFiles = rawFiles.filterIsInstance<KtFile>().run {
        if (shouldSortInputFiles) {
            sortedBy { it.name }
        } else this
    }
    return ModuleWithScopeProvider(sourceModule) { analysisSession ->
        with(analysisSession) {
            ktFiles.map { it.getFileSymbol().getFileScope() }
        }
    }
}