/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
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
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportInput
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.io.path.Path

internal fun buildSwiftModule(
    input: SwiftExportInput,
    kotlinDistribution: Distribution,
    shouldSortInputFiles: Boolean,
    bridgeModuleName: String,
): SirModule {
    val (module, ktFiles) = extractModuleWithFiles(kotlinDistribution, input, shouldSortInputFiles)

    return analyze(module) {
        val sirSession = StandaloneSirSession(this) { _, _ ->
            SirSingleModuleProvider(swiftModuleName = input.moduleName, bridgeModuleName = bridgeModuleName)
        }
        with(sirSession) {
            module.sirModule().also {
                ktFiles
                    .flatMap {
                        it.getFileSymbol().getFileScope()
                            .extractDeclarations()
                    }
                    .forEach { topLevelDeclaration ->
                        val parent = topLevelDeclaration.parent as? SirMutableDeclarationContainer
                            ?: error("top level declaration can contain only module or extension to package as a parent")
                        parent.addChild { topLevelDeclaration }
                    }
            }
        }
    }
}

@OptIn(KtAnalysisApiInternals::class)
private fun extractModuleWithFiles(
    kotlinDistribution: Distribution,
    input: SwiftExportInput,
    shouldSortInputFiles: Boolean,
): Pair<KtSourceModule, List<KtFile>> {
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
                    addSourceRoot(input.sourceRoot)
                    platform = NativePlatforms.unspecifiedNativePlatform
                    moduleName = input.moduleName
                    addRegularDependency(stdlib)
                }
            )
        }
    }

    val (sourceModule, rawFiles) = analysisAPISession.modulesWithFiles.entries.single()

    var ktFiles = rawFiles.filterIsInstance<KtFile>()

    if (shouldSortInputFiles) {
        ktFiles = ktFiles.sortedBy { it.name }
    }

    return Pair(sourceModule, ktFiles)
}