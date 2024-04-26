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
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirMutableDeclarationContainer
import org.jetbrains.kotlin.sir.providers.impl.SirSingleModuleProvider
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.klib.KlibScope
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.io.path.Path

internal fun buildSwiftModule(
    input: InputModule,
    config: SwiftExportConfig,
): SirModule {
    val (module, scopeProvider) = when (input) {
        is InputModule.Source -> createModuleWithScopeProviderFromSources(config.distribution, input)
        is InputModule.Binary -> createModuleWithScopeProviderFromBinary(config.distribution, input)
    }

    return analyze(module) {
        val sirSession = StandaloneSirSession(
            module,
            errorTypeStrategy = config.errorTypeStrategy.toInternalType(),
            unsupportedTypeStrategy = config.unsupportedTypeStrategy.toInternalType(),
            moduleProviderBuilder = { SirSingleModuleProvider(swiftModuleName = input.name) }
        )
        with(sirSession) {
            module.sirModule().also {
                scopeProvider(this@analyze).flatMap { scope ->
                    scope.extractDeclarations(this@analyze)
                }.forEach { topLevelDeclaration ->
                    val parent = topLevelDeclaration.parent as? SirMutableDeclarationContainer
                        ?: error("top level declaration can contain only module or extension to package as a parent")
                    parent.addChild { topLevelDeclaration }
                }
            }
        }
    }
}

private data class ModuleWithScopeProvider(
    val ktModule: KtModule,
    val scopeProvider: (KtAnalysisSession) -> List<KtScope>
)

@OptIn(KtAnalysisApiInternals::class)
private fun createModuleWithScopeProviderFromSources(
    kotlinDistribution: Distribution,
    input: InputModule.Source,
): ModuleWithScopeProvider {
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

            addModule(
                buildKtSourceModule {
                    addSourceRoot(input.path)
                    platform = NativePlatforms.unspecifiedNativePlatform
                    moduleName = input.name
                    addRegularDependency(stdlib)
                }
            )
        }
    }

    val (sourceModule, rawFiles) = analysisAPISession.modulesWithFiles.entries.single()
    return ModuleWithScopeProvider(sourceModule) { analysisSession ->
        with(analysisSession) {
            rawFiles.filterIsInstance<KtFile>().map { it.getFileSymbol().getFileScope() }
        }
    }
}

@OptIn(KtAnalysisApiInternals::class)
private fun createModuleWithScopeProviderFromBinary(
    kotlinDistribution: Distribution,
    input: InputModule.Binary,
): ModuleWithScopeProvider {
    lateinit var binaryModule: KtLibraryModule
    lateinit var fakeSourceModule: KtSourceModule
    buildStandaloneAnalysisAPISession {
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
            binaryModule = addModule(
                buildKtLibraryModule {
                    addBinaryRoot(input.path)
                    platform = NativePlatforms.unspecifiedNativePlatform
                    libraryName = input.name
                    addRegularDependency(stdlib)
                }
            )
            // It's a pure hack: Analysis API does not properly work without root source modules.
            fakeSourceModule = addModule(
                buildKtSourceModule {
                    platform = NativePlatforms.unspecifiedNativePlatform
                    moduleName = "fakeSourceModule"
                    addRegularDependency(binaryModule)
                    addRegularDependency(stdlib)
                }
            )
        }
    }
    return ModuleWithScopeProvider(fakeSourceModule) { analysisSession ->
        listOf(KlibScope(binaryModule, analysisSession))
    }
}