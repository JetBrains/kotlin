/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.extractDeclarations
import org.jetbrains.kotlin.sir.providers.impl.SirKaClassReferenceHandler
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.trampolineDeclarations
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.klib.getAllDeclarations
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession

internal fun buildSirSession(
    mainModuleName: String,
    kaModules: KaModules,
    config: SwiftExportConfig,
    moduleConfig: SwiftModuleConfig,
    referenceHandler: SirKaClassReferenceHandler? = null,
): SirSession = StandaloneSirSession(
    useSiteModule = kaModules.useSiteModule,
    moduleToTranslate = kaModules.mainModules.single { it.libraryName == mainModuleName },
    errorTypeStrategy = config.errorTypeStrategy.toInternalType(),
    unsupportedTypeStrategy = config.unsupportedTypeStrategy.toInternalType(),
    moduleForPackageEnums = buildModule { name = config.moduleForPackagesName },
    unsupportedDeclarationReporter = moduleConfig.unsupportedDeclarationReporter,
    moduleProvider = SirOneToOneModuleProvider(kaModules.platformLibraries),
    targetPackageFqName = moduleConfig.targetPackageFqName,
    referencedTypeHandler = referenceHandler,
    enableCoroutinesSupport = config.enableCoroutinesSupport,
)

/**
 * Translates the given [module] to a [SirModule].
 * The result is stored as a side effect in [this.sirSession]'s [org.jetbrains.kotlin.sir.providers.SirModuleProvider].
 * [scopeToDeclarations] allows filtering declarations during translation.
 */
context(sir: SirSession)
internal fun translateModule(
    module: KaLibraryModule,
    moduleToDeclarations: context(KaSession) (KaLibraryModule) -> Sequence<KaDeclarationSymbol> = { it.getAllDeclarations() },
): SirModule = analyze(sir.useSiteModule) {
    extractAllTransitively(moduleToDeclarations(module))
        .toList()
        .forEach { (oldParent, children) ->
            children
                .mapNotNull { declaration -> (declaration.parent as? SirMutableDeclarationContainer)?.let { it to declaration } }
                .forEach { (newParent, declaration) ->
                    (oldParent as? SirMutableDeclarationContainer)?.apply { declarations.remove(declaration) }
                    newParent.addChild { declaration }
                }
        }
    return@analyze module.sirModule()
}

context(sir: SirSession)
private fun extractAllTransitively(
    declarations: Sequence<KaDeclarationSymbol>,
): Sequence<Pair<SirDeclarationParent, List<SirDeclaration>>> = generateSequence(
    declarations.extractDeclarations()
        .flatMap { listOf(it) + it.trampolineDeclarations() }
        .groupBy { it.parent }.toList()
) {
    it.flatMap { (_, children) ->
        children
            .filterIsInstance<SirDeclarationContainer>()
            .map { it to it.declarations }
    }.takeIf { it.isNotEmpty() }
}.flatten()

internal typealias KaModules = org.jetbrains.kotlin.analysis.api.klib.reader.KaModules<SwiftModuleConfig>
