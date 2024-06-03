/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirMutableDeclarationContainer
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.sir.util.isValidSwiftIdentifier
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEFAULT_BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportLogger
import org.jetbrains.kotlin.swiftexport.standalone.klib.KlibScope
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.io.path.Path

internal class SwiftModuleBuildResults(
    val mainModule: SirModule,
    val moduleForPackageEnums: SirModule,
)

internal fun buildSwiftModule(
    input: InputModule.Binary,
    config: SwiftExportConfig,
    unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
): SwiftModuleBuildResults {
    val (useSiteModule, mainModule, scopeProvider) =
        createModuleWithScopeProviderFromBinary(config.distribution, input)
    val moduleForPackageEnums = buildModule {
        name = input.name
    }
    val sirOneToOneModuleProvider = SirOneToOneModuleProvider(mainModuleName = input.name)
    analyze(useSiteModule) {
        val sirSession = StandaloneSirSession(
            useSiteModule,
            errorTypeStrategy = config.errorTypeStrategy.toInternalType(),
            unsupportedTypeStrategy = config.unsupportedTypeStrategy.toInternalType(),
            moduleForPackageEnums = moduleForPackageEnums,
            unsupportedDeclarationReporter = unsupportedDeclarationReporter,
            moduleProviderBuilder = { sirOneToOneModuleProvider },
            targetPackageFqName = config.settings[SwiftExportConfig.ROOT_PACKAGE]?.let { packageName ->
                packageName.takeIf { FqNameUnsafe.isValid(it) }?.let { FqName(it) }
                    ?.takeIf { it.pathSegments().all { it.toString().isValidSwiftIdentifier() } }
                    ?: null.also {
                        config.logger.report(
                            SwiftExportLogger.Severity.Warning,
                            "'$packageName' is not a valid name for ${SwiftExportConfig.ROOT_PACKAGE} and will be ignored"
                        )
                    }
            },
        )
        with(sirSession) {
            scopeProvider(this@analyze).flatMap { scope ->
                scope.extractDeclarations(this@analyze)
            }.forEach { topLevelDeclaration ->
                val parent = topLevelDeclaration.parent as? SirMutableDeclarationContainer
                    ?: error("top level declaration can contain only module or extension to package as a parent")
                parent.addChild { topLevelDeclaration }
            }
        }
    }
    val mainSirModule = sirOneToOneModuleProvider.modules.getOrElse(mainModule) {
        // This branch is triggered when the [mainModule] is empty.
        buildModule { name = input.name }
    }
    return SwiftModuleBuildResults(mainSirModule, moduleForPackageEnums)
}

/**
 * Post-processed result of [buildStandaloneAnalysisAPISession].
 * [useSiteModule] is the module that should be passed to [analyze].
 * [mainModule] is the parent for declarations from [scopeProvider].
 * We have to make this difference because Analysis API is not suited to work
 * without root source module (yet?).
 * [scopeProvider] provides declarations that should be worked with.
 */
private data class ModuleWithScopeProvider(
    val useSiteModule: KtModule,
    val mainModule: KtModule,
    val scopeProvider: (KaSession) -> List<KaScope>,
)

private fun createModuleWithScopeProviderFromBinary(
    kotlinDistribution: Distribution,
    input: InputModule.Binary,
): ModuleWithScopeProvider {
    lateinit var binaryModule: KtLibraryModule
    lateinit var fakeSourceModule: KtSourceModule
    buildStandaloneAnalysisAPISession {
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
    return ModuleWithScopeProvider(fakeSourceModule, binaryModule) { analysisSession ->
        listOf(KlibScope(binaryModule, analysisSession))
    }
}
