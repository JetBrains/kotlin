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
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildImport
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportInput
import org.jetbrains.sir.passes.builder.buildSirDeclarationList
import kotlin.io.path.Path

@OptIn(KtAnalysisApiInternals::class)
internal fun buildSwiftModule(
    input: SwiftExportInput,
    kotlinDistribution: Distribution,
    shouldSortInputFiles: Boolean,
    bridgeModuleName: String,
): SirModule {
    val session = buildStandaloneAnalysisAPISession {
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
                    moduleName = "main"
                    addRegularDependency(stdlib)
                }
            )
        }
    }

    val (sourceModule, rawFiles) = session.modulesWithFiles.entries.single()

    var ktFiles = rawFiles.filterIsInstance<KtFile>()

    if (shouldSortInputFiles) {
        ktFiles = ktFiles.sortedBy { it.name }
    }

    return analyze(sourceModule) {
        buildModule {
            name = sourceModule.moduleName
            declarations += buildImport {
                moduleName = bridgeModuleName
            }
            ktFiles.forEach { file ->
                declarations += buildSirDeclarationList(file)
            }
        }.apply {
            declarations.forEach { it.parent = this }
        }
    }
}