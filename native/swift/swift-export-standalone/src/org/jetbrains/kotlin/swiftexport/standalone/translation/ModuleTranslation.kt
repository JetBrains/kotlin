/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.translation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.builders.SwiftExportDependencies
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.createKaModulesForStandaloneAnalysis
import org.jetbrains.kotlin.swiftexport.standalone.builders.initializeSirModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.utils.StandaloneSirTypeNamer
import org.jetbrains.kotlin.swiftexport.standalone.writer.BridgeSources
import org.jetbrains.kotlin.swiftexport.standalone.writer.generateBridgeSources

/**
 * Translates the whole public API surface of the given [module] to [SirModule] and generates compiler bridges between them.
 */
internal fun translateModulePublicApi(
    module: InputModule,
    dependencies: SwiftExportDependencies<InputModule>,
    config: SwiftExportConfig
): TranslationResult {
    val moduleWithScopeProvider = createKaModulesForStandaloneAnalysis(module, config.targetPlatform, dependencies)
    // We access KaSymbols through all the module translation process. Since it is not correct to access them directly
    // outside of the session they were created, we create KaSession here.
    return analyze(moduleWithScopeProvider.useSiteModule) {
        val buildResult = initializeSirModule(
            moduleWithScopeProvider,
            config,
            module.config,
            SirOneToOneModuleProvider(moduleWithScopeProvider.dependencies.platform)
        )
        // Assume that parts of the KotlinRuntimeSupport module are used.
        // It might not be the case, but precise tracking seems like an overkill at the moment.
        buildResult.module.updateImport(SirImport(config.runtimeSupportModuleName))
        buildResult.module.updateImport(SirImport(config.runtimeModuleName))
        val bridges = generateModuleBridges(buildResult.module, module.bridgesModuleName)
        TranslationResult(
            packages = buildResult.packages,
            sirModule = buildResult.module,
            bridgeSources = bridges,
            moduleConfig = module.config,
            bridgesModuleName = module.bridgesModuleName,
        )
    }
}

/**
 * Generates method bodies for functions in [sirModule], as well as Kotlin and C [BridgeSources].
 */
private fun KaSession.generateModuleBridges(sirModule: SirModule, bridgeModuleName: String): BridgeSources {
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

private val InputModule.bridgesModuleName: String
    get() = "${config.bridgeModuleName}_${name}"

internal class TranslationResult(
    val sirModule: SirModule,
    val packages: Set<FqName>,
    val bridgeSources: BridgeSources,
    val moduleConfig: SwiftModuleConfig,
    val bridgesModuleName: String,
)
