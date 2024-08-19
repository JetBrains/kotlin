/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirMutableDeclarationContainer
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.swiftexport.standalone.klib.KlibScope
import org.jetbrains.kotlin.swiftexport.standalone.session.StandaloneSirSession
import kotlin.io.path.Path

internal class SwiftModuleBuildResults(
    val module: SirModule,
    val packages: Set<FqName>,
)

internal fun ModuleWithScopeProvider.initializeSirModule(
    config: SwiftExportConfig,
    moduleProvider: SirModuleProvider,
): SwiftModuleBuildResults {
    val moduleForPackageEnums = when (config.multipleModulesHandlingStrategy) {
        MultipleModulesHandlingStrategy.OneToOneModuleMapping -> buildModule { name = config.moduleForPackagesName }
        MultipleModulesHandlingStrategy.IntoSingleModule -> with(moduleProvider) { mainModule.sirModule() }
    }
    val sirSession = StandaloneSirSession(
        useSiteModule = useSiteModule,
        moduleToTranslate = mainModule,
        errorTypeStrategy = config.errorTypeStrategy.toInternalType(),
        unsupportedTypeStrategy = config.unsupportedTypeStrategy.toInternalType(),
        moduleForPackageEnums = moduleForPackageEnums,
        unsupportedDeclarationReporter = config.unsupportedDeclarationReporter,
        moduleProvider = moduleProvider,
        targetPackageFqName = config.targetPackageFqName,
    )

    // this lines produce critical side effect
    // This will traverse every top level declaration of a given provider
    // This in turn inits every root declaration that will be consumed down the pipe by swift export
    traverseTopLevelDeclarationsInScopes(sirSession, scopeProvider)
    val module = with(moduleProvider) {
        mainModule.sirModule().apply {
            with(sirSession) {
                dumpAdapterDeclarations()
            }
        }
    }

    return SwiftModuleBuildResults(
        module = module,
        packages = if (config.multipleModulesHandlingStrategy == MultipleModulesHandlingStrategy.OneToOneModuleMapping)
            sirSession.enumGenerator.collectedPackages
        else
            emptySet()
    )
}

private fun traverseTopLevelDeclarationsInScopes(
    sirSession: StandaloneSirSession,
    scopeProvider: KaSession.() -> List<KaScope>,
) {
    with(sirSession) {
        analyze(useSiteModule) {
            scopeProvider().flatMap { scope ->
                scope.extractDeclarations(useSiteSession)
            }.forEach { topLevelDeclaration ->
                val parent = topLevelDeclaration.parent as? SirMutableDeclarationContainer
                    ?: error("top level declaration can contain only module or extension to package as a parent")
                parent.addChild { topLevelDeclaration }
            }
        }
    }
}

/**
 * Post-processed result of [buildStandaloneAnalysisAPISession].
 * [useSiteModule] is the module that should be passed to [analyze].
 * [mainModule] is the parent for declarations from [scopeProvider].
 * We have to make this difference because Analysis API is not suited to work
 * without root source module (yet?).
 * [scopeProvider] provides declarations that should be worked with.
 */
internal data class ModuleWithScopeProvider(
    val useSiteModule: KaModule,
    val mainModule: KaModule,
    val scopeProvider: KaSession.() -> List<KaScope>,
)

internal fun createModuleWithScopeProviderFromBinary(
    input: InputModule,
    dependencies: Set<InputModule>,
): ModuleWithScopeProvider {
    lateinit var binaryModule: KaLibraryModule
    lateinit var fakeSourceModule: KaSourceModule
    buildStandaloneAnalysisAPISession {
        buildKtModuleProvider {
            platform = NativePlatforms.unspecifiedNativePlatform

            val stdlib = addModule(
                buildKtLibraryModule {
                    addBinaryRoot(Path(input.config.distribution.stdlib))
                    platform = NativePlatforms.unspecifiedNativePlatform
                    libraryName = "stdlib"
                }
            )
            binaryModule = addModule(addModuleForSwiftExportConsumption(input, stdlib))
            val kaDeps = dependencies.map {
                addModule(addModuleForSwiftExportConsumption(it, stdlib))
            }
            // It's a pure hack: Analysis API does not properly work without root source modules.
            fakeSourceModule = addModule(
                buildKtSourceModule {
                    platform = NativePlatforms.unspecifiedNativePlatform
                    moduleName = "fakeSourceModule"
                    addRegularDependency(binaryModule)
                    addRegularDependency(stdlib)
                    kaDeps.forEach { addRegularDependency(it) }
                }
            )
        }
    }
    return ModuleWithScopeProvider(fakeSourceModule, binaryModule) {
        listOf(KlibScope(binaryModule, useSiteSession))
    }
}

private fun KtModuleProviderBuilder.addModuleForSwiftExportConsumption(
    input: InputModule,
    stdlib: KaLibraryModule,
): KaLibraryModule = buildKtLibraryModule {
    addBinaryRoot(input.path)
    platform = NativePlatforms.unspecifiedNativePlatform
    libraryName = input.name
    addRegularDependency(stdlib)
}
