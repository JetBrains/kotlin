/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.translation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule
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
import org.jetbrains.kotlin.swiftexport.standalone.builders.SwiftExportDependencies
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSirSession
import org.jetbrains.kotlin.swiftexport.standalone.builders.createKaModulesForStandaloneAnalysis
import org.jetbrains.kotlin.swiftexport.standalone.builders.translateModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.utils.StandaloneSirTypeNamer
import org.jetbrains.kotlin.swiftexport.standalone.writer.BridgeSources
import org.jetbrains.kotlin.swiftexport.standalone.writer.generateBridgeSources
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import kotlin.collections.contains

/**
 * Translates the whole public API surface of the given [module] to [SirModule] and generates compiler bridges between them.
 */
@OptIn(KaExperimentalApi::class)
internal fun translateModulePublicApi(
    module: InputModule,
    dependencies: SwiftExportDependencies<InputModule>,
    config: SwiftExportConfig
): TranslationResult {
    val kaModules = createKaModulesForStandaloneAnalysis(module, config.targetPlatform, dependencies)
    // We access KaSymbols through all the module translation process. Since it is not correct to access them directly
    // outside of the session they were created, we create KaSession here.
    return analyze(kaModules.useSiteModule) {
        val referencedStdlibTypes = mutableSetOf<FqName>()
        val stdlibReferenceHandler = SirKaClassReferenceHandler { symbol ->
            val symbolContainingModule = symbol.containingModule as? KaLibraryModule
            if (symbolContainingModule?.libraryName == "stdlib") {
                referencedStdlibTypes.addIfNotNull(symbol.classId?.outermostClassId?.asSingleFqName())
            }
        }
        val sirSession = buildSirSession(kaModules, config, module.config, stdlibReferenceHandler)
        translateModule(sirSession, kaModules.mainModule)
        createTranslationResult(sirSession, config, module.config, kaModules, referencedStdlibTypes)
    }
}

internal fun translateModuleTransitiveClosure(module: InputModule, config: SwiftExportConfig, names: Set<FqName>): TranslationResult {
    val kaModules = createKaModulesForStandaloneAnalysis(module, config.targetPlatform, null)
    return analyze(kaModules.useSiteModule) {
        // Accumulates all referenced stdlib types.
        val referencedStdlibTypes = names.toMutableSet()
        val newlyReferencedTypes = mutableSetOf<FqName>()
        val stdlibReferenceHandler = SirKaClassReferenceHandler { symbol ->
            val classId = symbol.classId?.asSingleFqName()
            if (classId != null) {
                // This check might be slow as the number of referenced types could grow significantly.
                if (classId !in referencedStdlibTypes) {
                    referencedStdlibTypes += classId
                    newlyReferencedTypes += classId
                }
            }
        }
        val sirSession = buildSirSession(kaModules, config, module.config, stdlibReferenceHandler)
        var inputQueue = names
        do {
            translateModule(sirSession, kaModules.mainModule) { scope ->
                scope.classifiers
                    .filterIsInstance<KaClassLikeSymbol>()
                    .filter { it.classId?.asSingleFqName() in inputQueue }
            }
            inputQueue = newlyReferencedTypes.toSet()
            newlyReferencedTypes.clear()
        } while (inputQueue.isNotEmpty())
        createTranslationResult(sirSession, config, module.config, kaModules, emptySet())
    }
}

private fun KaSession.createTranslationResult(
    sirSession: SirSession,
    config: SwiftExportConfig,
    moduleConfig: SwiftModuleConfig,
    kaModules: KaModules,
    referencedStdlibTypes: Set<FqName>,
): TranslationResult {
    val sirModule = with(sirSession) { kaModules.mainModule.sirModule() }
    // Assume that parts of the KotlinRuntimeSupport and KotlinRuntime module are used.
    // It might not be the case, but precise tracking seems like an overkill at the moment.
    sirModule.updateImport(SirImport(config.runtimeSupportModuleName))
    sirModule.updateImport(SirImport(config.runtimeModuleName))
    val bridgesName = "${moduleConfig.bridgeModuleName}_${sirModule.name}"
    val bridges = sirSession.withSessions { generateModuleBridges(sirModule, bridgesName) }
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
        referencedStdlibTypes = referencedStdlibTypes,
    )
}

/**
 * Generates method bodies for functions in [sirModule], as well as Kotlin and C [BridgeSources].
 */
private fun SirAndKaSession.generateModuleBridges(sirModule: SirModule, bridgeModuleName: String): BridgeSources {
    val bridgeGenerator = createBridgeGenerator(StandaloneSirTypeNamer)

    // KT-68253: bridge generation could be better
    val bridgeRequests = buildBridgeRequests(bridgeGenerator, sirModule)
    if (bridgeRequests.isNotEmpty()) {
        sirModule.updateImport(
            SirImport(
                moduleName = bridgeModuleName,
                mode = SirImport.Mode.ImplementationOnly
            )
        )
    }
    return generateBridgeSources(bridgeGenerator, bridgeRequests, true)
}

internal class TranslationResult(
    val swiftModuleName: String,
    val swiftModuleSources: String,
    val referencedSwiftModules: List<SwiftExportModule.Reference>,
    val packages: Set<FqName>,
    val bridgeSources: BridgeSources,
    val moduleConfig: SwiftModuleConfig,
    val bridgesModuleName: String,
    val referencedStdlibTypes: Set<FqName>,
)
