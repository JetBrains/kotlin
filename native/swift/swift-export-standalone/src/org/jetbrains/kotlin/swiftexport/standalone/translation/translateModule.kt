/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.translation

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.providers.impl.SirKaClassReferenceHandler
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.createModuleWithScopeProviderFromBinary
import org.jetbrains.kotlin.swiftexport.standalone.builders.initializeSirModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.utils.StandaloneSirTypeNamer
import org.jetbrains.kotlin.swiftexport.standalone.writer.generateBridgeSources
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun translateModule(
    module: InputModule,
    dependencies: Set<InputModule>,
    config: SwiftExportConfig,
    referencedKotlinClassifiers: MutableSet<FqName>
): TranslationResult {
    val moduleWithScopeProvider = createModuleWithScopeProviderFromBinary(module, config.distribution.stdlib, dependencies)
    // We access KaSymbols through all the module translation process. Since it is not correct to access them directly
    // outside of the session they were created, we create KaSession here.
    return analyze(moduleWithScopeProvider.useSiteModule) {

        val stdlibReferencesCollector = SirKaClassReferenceHandler { kaClass ->
            val containingModule = kaClass.containingModule
            if (containingModule !is KaLibraryModule || containingModule.libraryName != "stdlib") {
                return@SirKaClassReferenceHandler
            }
            // Add only top-level declarations to the working queue.
            referencedKotlinClassifiers.addIfNotNull(kaClass.classId?.outermostClassId?.asSingleFqName())
        }

        val buildResult = initializeSirModule(moduleWithScopeProvider, config, module.config, SirOneToOneModuleProvider(), stdlibReferencesCollector)

        // Assume that parts of the KotlinRuntimeSupport module are used.
        // It might not be the case, but precise tracking seems like an overkill at the moment.
        buildResult.module.updateImport(SirImport(config.runtimeSupportModuleName))

        val bridgeGenerator = createBridgeGenerator(StandaloneSirTypeNamer)

        // KT-68253: bridge generation could be better
        val bridgeRequests = buildBridgeRequests(bridgeGenerator, buildResult.module)
        if (bridgeRequests.isNotEmpty()) {
            buildResult.module.updateImport(
                SirImport(
                    moduleName = module.bridgesModuleName,
                    mode = SirImport.Mode.ImplementationOnly
                )
            )
        }

        val bridges = generateBridgeSources(bridgeGenerator, bridgeRequests, true)
        TranslationResult(
            packages = buildResult.packages,
            sirModule = buildResult.module,
            bridgeSources = bridges,
            config = config,
            moduleConfig = module.config,
            bridgesModuleName = module.bridgesModuleName,
        )
    }
}

private val InputModule.bridgesModuleName: String
    get() = "${config.bridgeModuleName}_${name}"