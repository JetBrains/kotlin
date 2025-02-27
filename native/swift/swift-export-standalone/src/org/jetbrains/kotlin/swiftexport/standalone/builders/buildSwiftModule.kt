/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.SirKaClassReferenceHandler
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.klib.KlibScope
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.sequences.forEach

internal fun buildSirSession(
    kaModules: KaModules,
    config: SwiftExportConfig,
    moduleConfig: SwiftModuleConfig,
    referenceHandler: SirKaClassReferenceHandler? = null
): StandaloneSirSession =
    StandaloneSirSession(
        useSiteModule = kaModules.useSiteModule,
        moduleToTranslate = kaModules.mainModule,
        errorTypeStrategy = config.errorTypeStrategy.toInternalType(),
        unsupportedTypeStrategy = config.unsupportedTypeStrategy.toInternalType(),
        moduleForPackageEnums = buildModule { name = config.moduleForPackagesName },
        unsupportedDeclarationReporter = moduleConfig.unsupportedDeclarationReporter,
        moduleProvider = SirOneToOneModuleProvider(),
        targetPackageFqName = moduleConfig.targetPackageFqName,
        referencedTypeHandler = referenceHandler
    )

internal fun KaSession.translateModule(
    sirSession: SirSession,
    module: KaLibraryModule,
    scopeToDeclarations: (KaScope) -> Sequence<KaDeclarationSymbol> = { it.declarations },
) {
    val scope = KlibScope(module, useSiteSession)
    extractAllTransitively(scopeToDeclarations(scope), sirSession, useSiteSession)
        .mapNotNull { declaration -> (declaration.parent as? SirMutableDeclarationContainer)?.let { it to declaration } }
        .forEach { (parent, declaration) -> parent.addChild { declaration } }
}

private fun extractAllTransitively(
    declarations: Sequence<KaDeclarationSymbol>,
    sirSession: SirSession,
    kaSession: KaSession,
): Sequence<SirDeclaration> = with(sirSession) {
    generateSequence(declarations.extractDeclarations(kaSession)) {
        it.filterIsInstance<SirDeclarationContainer>().flatMap { it.declarations }
    }.flatten()
}

/**
 * Post-processed result of [buildStandaloneAnalysisAPISession].
 * [useSiteModule] is the module that should be passed to [analyze].
 * [mainModule] is the parent for declarations from [scopeProvider].
 * We have to make this difference because Analysis API is not suited to work
 * without a root source module (yet?).
 */
internal class KaModules(
    val useSiteModule: KaModule,
    val mainModule: KaLibraryModule,
)

internal fun createKaModulesForStandaloneAnalysis(
    input: InputModule,
    dependencies: Set<InputModule>,
): KaModules {
    lateinit var binaryModule: KaLibraryModule
    lateinit var fakeSourceModule: KaSourceModule
    buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = NativePlatforms.unspecifiedNativePlatform
            binaryModule = addModule(buildKaLibraryModule(input))
            val kaDeps = dependencies.map {
                addModule(buildKaLibraryModule(it))
            }
            // It's a pure hack: Analysis API does not properly work without root source modules.
            fakeSourceModule = addModule(
                buildKtSourceModule {
                    platform = NativePlatforms.unspecifiedNativePlatform
                    moduleName = "fakeSourceModule"
                    addRegularDependency(binaryModule)
                    kaDeps.forEach { addRegularDependency(it) }
                }
            )
        }
    }
    return KaModules(fakeSourceModule, binaryModule)
}

private fun KtModuleProviderBuilder.buildKaLibraryModule(
    input: InputModule,
): KaLibraryModule = buildKtLibraryModule {
    addBinaryRoot(input.path)
    platform = NativePlatforms.unspecifiedNativePlatform
    libraryName = input.name
}