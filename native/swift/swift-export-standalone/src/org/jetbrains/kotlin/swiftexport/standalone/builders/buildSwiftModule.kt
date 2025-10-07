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
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.extractDeclarations
import org.jetbrains.kotlin.sir.providers.impl.SirKaClassReferenceHandler
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.trampolineDeclarations
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftexport.standalone.*
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


/**
 * [useSiteModule] a target for creating Analysis API session via [analyze].
 * [mainModules] Kotlin modules, which _might_ be translated to Swift.
 * [platformLibraries] Platform libraries from the Kotlin Native distribution.
 */
internal class KaModules(
    val useSiteModule: KaModule,
    private val modulesToInputs: Map<KaLibraryModule, InputModule>,
    val platformLibraries: List<KaLibraryModule>,
) {
    val inputsToModules: Map<InputModule, KaLibraryModule> = modulesToInputs.map { it.value to it.key }.toMap()
    val mainModules: List<KaLibraryModule> = modulesToInputs.keys.toList()
    fun configFor(module: KaLibraryModule): SwiftModuleConfig =
        modulesToInputs[module]?.config ?: error("No config for module ${module.libraryName}")
}

internal fun createKaModulesForStandaloneAnalysis(
    inputs: Set<InputModule>,
    targetPlatform: TargetPlatform,
    platformLibraries: Set<InputModule>,
): KaModules {
    lateinit var binaryModules: Map<KaLibraryModule, InputModule>
    lateinit var fakeSourceModule: KaSourceModule
    var platformLibraryModules: List<KaLibraryModule> = emptyList()
    buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = targetPlatform
            binaryModules = inputs.associate { inputModuleIntoKaLibraryModule(it, targetPlatform) to it }
            platformLibraryModules = platformLibraries.map { inputModuleIntoKaLibraryModule(it, targetPlatform) }
            // It's a pure hack: Analysis API does not properly work without root source modules.
            fakeSourceModule = addModule(
                buildKtSourceModule {
                    platform = targetPlatform
                    moduleName = "fakeSourceModule"
                    binaryModules.forEachKey(::addRegularDependency)
                    platformLibraryModules.forEach(::addRegularDependency)
                }
            )
        }
    }
    return KaModules(fakeSourceModule, binaryModules, platformLibraryModules)
}

private fun KtModuleProviderBuilder.inputModuleIntoKaLibraryModule(
    input: InputModule,
    targetPlatform: TargetPlatform,
): KaLibraryModule = addModule(
    buildKtLibraryModule {
        addBinaryRoot(input.path)
        platform = targetPlatform
        libraryName = input.name
    }
)

private inline fun <K> Map<out K, *>.forEachKey(action: (K) -> Unit) = keys.forEach(action)
