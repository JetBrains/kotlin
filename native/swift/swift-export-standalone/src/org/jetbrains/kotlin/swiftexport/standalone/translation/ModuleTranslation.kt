/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.translation

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.providers.SirAndKaSession
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.SirKaClassReferenceHandler
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.builders.KaModules
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSirSession
import org.jetbrains.kotlin.swiftexport.standalone.builders.translateModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.utils.StandaloneSirTypeNamer
import org.jetbrains.kotlin.swiftexport.standalone.writer.BridgeSources
import org.jetbrains.kotlin.swiftexport.standalone.writer.generateBridgeSources
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import kotlin.collections.contains
import kotlin.collections.plusAssign

/**
 * Translates the whole public API surface of the given [module] to [SirModule] and generates compiler bridges between them.
 */
internal fun translateModulePublicApi(module: InputModule, kaModules: KaModules, config: SwiftExportConfig): TranslationResult {
    val bridgeGenerator = createBridgeGenerator(StandaloneSirTypeNamer)
    // We access KaSymbols through all the module translation process. Since it is not correct to access them directly
    // outside of the session they were created, we create KaSession here.
    return analyze(kaModules.useSiteModule) {
        val externalTypeDeclarationReferences = mutableMapOf<KaLibraryModule, MutableList<FqName>>()
        val externalTypeReferenceHandler = SirKaClassReferenceHandler { symbol ->
            val symbolContainingModule = symbol.containingModule as? KaLibraryModule
            symbolContainingModule?.let { libraryName ->
                externalTypeDeclarationReferences
                    .getOrPut(libraryName) { mutableListOf() }
                    .addIfNotNull(symbol.classId?.asSingleFqName())
            }
        }
        buildSirSession(module.name, kaModules, config, module.config, externalTypeReferenceHandler).withSessions {
            val sirModule = translateModule(
                module = kaModules.mainModules.single { it.libraryName == module.name }
            )
            val bridgeRequests = sirSession.withSessions { buildBridgeRequests(bridgeGenerator, sirModule) }
            sirSession.createTranslationResult(sirModule, config, module.config, externalTypeDeclarationReferences, bridgeRequests)
        }
    }
}

/**
 * Accumulates module translation state during transitive export.
 */
private class ModuleTransitiveTranslationState(
    val kaModule: KaLibraryModule,
    val moduleConfig: SwiftModuleConfig,
    val unprocessedReferences: MutableSet<FqName>,
    val processedReferences: MutableSet<FqName>,
    val bridgeRequests: MutableList<BridgeRequest> = mutableListOf(),
) {
    lateinit var sirSession: SirSession
}

/**
 * Translate a group of cross-referencing [transitivelyExportedModules] into Swift.
 * Unlike [translateModulePublicApi], this function translates only referenced type declarations, leaving
 * top-level functions, properties, and unreferenced type declarations untouched.
 * [typeDeclarationReferences] acts as a root for the translation process.
 *
 * @return a list of non-empty [TranslationResult].
 */
internal fun translateCrossReferencingModulesTransitively(
    typeDeclarationReferences: Map<KaLibraryModule, List<FqName>>,
    kaModules: KaModules,
    config: SwiftExportConfig,
): List<TranslationResult> {
    val bridgeGenerator = createBridgeGenerator(StandaloneSirTypeNamer)
    val translationStates = typeDeclarationReferences
        .map { (module, references) ->
            ModuleTransitiveTranslationState(
                kaModule = module,
                moduleConfig = kaModules.configFor(module),
                unprocessedReferences = references.toMutableSet(),
                processedReferences = mutableSetOf(),
            )
        }
    val typeReferenceHandler = SirKaClassReferenceHandler { symbol ->
        analyze(kaModules.useSiteModule) {
            val libraryName = (symbol.containingModule as? KaLibraryModule)?.libraryName
            translationStates.find { it.kaModule.libraryName == libraryName }?.let {
                val fqName = symbol.classId?.asSingleFqName()
                    ?: return@analyze
                if (fqName !in it.processedReferences) {
                    it.unprocessedReferences += fqName
                }
            }
        }
    }
    translationStates.forEach {
        it.sirSession = buildSirSession(it.kaModule.libraryName, kaModules, config, it.moduleConfig, typeReferenceHandler)
    }
    // Translate modules until the process converges.
    while (translationStates.any { it.unprocessedReferences.isNotEmpty() }) {
        translationStates
            .filter { it.unprocessedReferences.isNotEmpty() }
            .map {
                // Copy new references to avoid concurrent modification exception.
                val inputQueue = it.unprocessedReferences.toList()
                it.unprocessedReferences.clear()
                val sirModule = it.sirSession.withSessions {
                    translateModule(module = it.kaModule) { scope ->
                        scope.classifiers
                            .filterIsInstance<KaClassLikeSymbol>()
                            .filter { it.classId?.asSingleFqName() in inputQueue }
                    }
                }
                it.processedReferences += inputQueue
                // We build bridges at every iteration as new references might appear.
                it.bridgeRequests += it.sirSession.withSessions { buildBridgeRequests(bridgeGenerator, sirModule) }
            }
    }
    return translationStates.mapNotNull {
        val sirModule = with(it.sirSession) { it.kaModule.sirModule() }
        // Avoid generation of empty modules.
        if (sirModule.declarations.isEmpty()) return@mapNotNull null
        it.sirSession.createTranslationResult(
            sirModule,
            config,
            it.moduleConfig,
            emptyMap(),
            it.bridgeRequests
        )
    }
}

private fun SirSession.createTranslationResult(
    sirModule: SirModule,
    config: SwiftExportConfig,
    moduleConfig: SwiftModuleConfig,
    externalTypeDeclarationReferences: Map<KaLibraryModule, List<FqName>>,
    bridgeRequests: List<BridgeRequest>,
): TranslationResult {
    // Assume that parts of the KotlinRuntimeSupport and KotlinRuntime module are used.
    // It might not be the case, but precise tracking seems like an overkill at the moment.
    sirModule.updateImport(SirImport(config.runtimeSupportModuleName))
    sirModule.updateImport(SirImport(config.runtimeModuleName))
    val bridgesName = "${moduleConfig.bridgeModuleName}_${sirModule.name}"
    val bridges = sirSession.withSessions { generateModuleBridges(sirModule, bridgesName, bridgeRequests) }
    // Serialize SirModule to sources to avoid leakage of SirSession (and KaSession, likely) outside the analyze call.
    val swiftSourceCode = SirAsSwiftSourcesPrinter.print(
        sirModule,
        config.stableDeclarationsOrder,
        config.renderDocComments,
    )
    val knownModuleNames = setOf(KotlinRuntimeModule.name, bridgesName) + config.platformLibsInputModule.map { it.name }
    val referencedSwiftModules = sirModule.imports
        .filter { it.moduleName !in knownModuleNames }
        .map { SwiftExportModule.Reference(it.moduleName) }
    return TranslationResult(
        swiftModuleName = sirModule.name,
        swiftModuleSources = swiftSourceCode,
        referencedSwiftModules = referencedSwiftModules,
        packages = sirSession.enumGenerator.collectedPackages,
        bridgeSources = bridges,
        moduleConfig = moduleConfig,
        bridgesModuleName = bridgesName,
        externalTypeDeclarationReferences = externalTypeDeclarationReferences,
    )
}

/**
 * Generates method bodies for functions in [sirModule], as well as Kotlin and C [BridgeSources].
 */
private fun SirAndKaSession.generateModuleBridges(sirModule: SirModule, bridgeModuleName: String, bridgeRequests: List<BridgeRequest>): BridgeSources {
    if (bridgeRequests.isNotEmpty()) {
        sirModule.updateImport(
            SirImport(
                moduleName = bridgeModuleName,
                mode = SirImport.Mode.ImplementationOnly
            )
        )
    }
    return generateBridgeSources(bridgeRequests, true)
}

internal class TranslationResult(
    val swiftModuleName: String,
    val swiftModuleSources: String,
    val referencedSwiftModules: List<SwiftExportModule.Reference>,
    val packages: Set<FqName>,
    val bridgeSources: BridgeSources,
    val moduleConfig: SwiftModuleConfig,
    val bridgesModuleName: String,
    val externalTypeDeclarationReferences: Map<KaLibraryModule, List<FqName>>,
)
