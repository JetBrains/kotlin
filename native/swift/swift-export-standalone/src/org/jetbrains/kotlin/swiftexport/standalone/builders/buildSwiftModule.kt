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
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.klib.KlibScope
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.sequences.forEach

internal class SwiftModuleBuildResults(
    val module: SirModule,
    val packages: Set<FqName>,
)

internal fun KaSession.initializeSirModule(
    moduleWithScope: KaModules,
    config: SwiftExportConfig,
    moduleConfig: SwiftModuleConfig,
    moduleProvider: SirModuleProvider,
): SwiftModuleBuildResults {
    val moduleForPackageEnums = buildModule { name = config.moduleForPackagesName }
    val sirSession = StandaloneSirSession(
        useSiteModule = moduleWithScope.useSiteModule,
        moduleToTranslate = moduleWithScope.mainModule,
        errorTypeStrategy = config.errorTypeStrategy.toInternalType(),
        unsupportedTypeStrategy = config.unsupportedTypeStrategy.toInternalType(),
        moduleForPackageEnums = moduleForPackageEnums,
        unsupportedDeclarationReporter = moduleConfig.unsupportedDeclarationReporter,
        moduleProvider = moduleProvider,
        targetPackageFqName = moduleConfig.targetPackageFqName,
    )

    // this lines produce critical side effect
    // This will traverse every top level declaration of a given provider
    // This in turn inits every root declaration that will be consumed down the pipe by swift export
    traverseTopLevelDeclarationsInScopes(sirSession, moduleWithScope.mainModule)

    return with(moduleProvider) {
        SwiftModuleBuildResults(
            module = moduleWithScope.mainModule.sirModule(),
            packages = sirSession.enumGenerator.collectedPackages,
        )
    }
}

private fun KaSession.traverseTopLevelDeclarationsInScopes(
    sirSession: StandaloneSirSession,
    module: KaLibraryModule,
) {
    KlibScope(module, useSiteSession).allDeclarations(sirSession, useSiteSession)
        .toList()
        .forEach { (oldParent, children) ->
            children
                .mapNotNull { declaration -> (declaration.parent as? SirMutableDeclarationContainer)?.let { it to declaration } }
                .forEach { (newParent, declaration) ->
                    (oldParent as? SirMutableDeclarationContainer)?.apply { declarations.remove(declaration) }
                    newParent.addChild { declaration }
                }
        }
}

private fun KaScope.allDeclarations(sirSession: StandaloneSirSession, kaSession: KaSession): Sequence<Pair<SirDeclarationParent, List<SirDeclaration>>> =
    with(sirSession) {
        generateSequence<List<Pair<SirDeclarationParent, List<SirDeclaration>>>>(this@allDeclarations.extractDeclarations(kaSession).groupBy { it.parent }.toList()) {
            it.flatMap { (_, children) ->
                children.filterIsInstance<SirDeclarationContainer>()
                    .map { it to it.declarations }
            }.takeIf { it.isNotEmpty() }
        }.flatMap { it }
    }

/**
 * Post-processed result of [buildStandaloneAnalysisAPISession].
 * [useSiteModule] is the module that should be passed to [analyze].
 * [mainModule] is the parent for declarations from [scopeProvider].
 * We have to make this difference because Analysis API is not suited to work
 * without a root source module (yet?).
 * [dependencies] are dependencies for the translated module.
 */
internal class KaModules(
    val useSiteModule: KaModule,
    val mainModule: KaLibraryModule,
    val dependencies: SwiftExportDependencies<KaLibraryModule>,
)

internal fun createKaModulesForStandaloneAnalysis(
    input: InputModule,
    targetPlatform: TargetPlatform,
    dependencies: SwiftExportDependencies<InputModule>,
): KaModules {
    lateinit var binaryModule: KaLibraryModule
    lateinit var fakeSourceModule: KaSourceModule
    lateinit var resultedDependencies: SwiftExportDependencies<KaLibraryModule>
    buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = targetPlatform
            binaryModule = inputModuleIntoKaLibraryModule(input, targetPlatform)
            resultedDependencies = dependencies.map { inputModuleIntoKaLibraryModule(it, targetPlatform) }
            // It's a pure hack: Analysis API does not properly work without root source modules.
            fakeSourceModule = addModule(
                buildKtSourceModule {
                    platform = targetPlatform
                    moduleName = "fakeSourceModule"
                    addRegularDependency(binaryModule)
                    resultedDependencies.forEach(::addRegularDependency)
                }
            )
        }
    }
    return KaModules(
        fakeSourceModule,
        binaryModule,
        resultedDependencies
    )
}

internal class SwiftExportDependencies<T>(
    val user: Set<T>,
    val stdlib: T,
    val platform: Set<T>,
) {
    inline fun <R> map(transform: (T) -> R) = SwiftExportDependencies(
        user = user.map(transform).toSet(),
        stdlib = transform(stdlib),
        platform = platform.map(transform).toSet(),
    )

    inline fun forEach(block: (T) -> Unit): Unit = (user + stdlib + platform).forEach(block)
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
