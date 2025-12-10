/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.flattenAndFilterOwnProviders
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.registerLibrarySessionComponents
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.StructuredProviders
import org.jetbrains.kotlin.fir.session.registerCliCompilerAndCommonComponents
import org.jetbrains.kotlin.fir.session.registerCommonComponentsAfterExtensionsAreConfigured
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import java.io.File

@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
internal fun createScriptingAdditionalLibrariesSession(
    libModuleData: FirModuleData,
    sessionFactoryContext: FirJvmSessionFactory.Context,
    moduleDataProvider: ModuleDataProvider,
    sharedLibrarySession: FirSession,
    extensionRegistrars: List<FirExtensionRegistrar>,
    compilerConfiguration: CompilerConfiguration,
    getKotlinClassFinder: () -> KotlinClassFinder,
    getJavaFacade: (FirSession) -> FirJavaFacade,
) : FirSession = FirCliSession(FirSession.Kind.Library).apply session@{
    libModuleData.bindSession(this@session)

    registerCliCompilerAndCommonComponents(compilerConfiguration.languageVersionSettings, false)
    registerLibrarySessionComponents(sessionFactoryContext)
    register(FirBuiltinSyntheticFunctionInterfaceProvider::class, sharedLibrarySession.syntheticFunctionInterfacesSymbolProvider)

    val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
    register(FirKotlinScopeProvider::class, kotlinScopeProvider)

    FirSessionConfigurator(this).apply {
        for (extensionRegistrar in extensionRegistrars) {
            registerExtensions(extensionRegistrar.configure())
        }
    }.configure()
    registerCommonComponentsAfterExtensionsAreConfigured()

    val providers = listOf(
        JvmClassFileBasedSymbolProvider(
            this@session,
            moduleDataProvider,
            kotlinScopeProvider,
            sessionFactoryContext.packagePartProviderForLibraries,
            getKotlinClassFinder(),
            getJavaFacade(this@session),
        )
    )
    register(
        StructuredProviders::class,
        StructuredProviders(
            sourceProviders = emptyList(),
            dependencyProviders = providers,
            sharedProvider = sharedLibrarySession.symbolProvider,
        )
    )

    val providersWithShared = providers + sharedLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

    val symbolProvider = FirCachingCompositeSymbolProvider(this, providersWithShared)
    register(FirSymbolProvider::class, symbolProvider)
    register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
}

internal fun configureLibrarySessionIfNeeded(
    state: K2ScriptingCompilerEnvironment,
    compilerConfiguration: CompilerConfiguration,
    classpath: List<File>,
): FirSession? {
    (state as? K2ScriptingCompilerEnvironmentInternal)
        ?: error("Expected the state of type K2ScriptingCompilerEnvironmentInternal, got ${state::class}")
    // needed for class finders for now anyway
    compilerConfiguration.addJvmClasspathRoots(classpath)
    state.compilerContext.environment.updateClasspath(classpath.map(::JvmClasspathRoot))
    val (libModuleData, _) = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpath.map(File::toPath))
    if (libModuleData != null) {
        val projectEnvironment = state.sessionFactoryContext.projectEnvironment
        val searchScope = state.moduleDataProvider.getModuleDataPaths(libModuleData)?.let { paths ->
            projectEnvironment.getSearchScopeByClassPath(paths)
        } ?: state.sessionFactoryContext.librariesScope

        return createScriptingAdditionalLibrariesSession(
            libModuleData,
            state.sessionFactoryContext,
            state.moduleDataProvider,
            state.sharedLibrarySession,
            state.extensionRegistrars,
            compilerConfiguration,
            getKotlinClassFinder = { projectEnvironment.getKotlinClassFinder(searchScope) },
            getJavaFacade = { projectEnvironment.getFirJavaFacade(it, libModuleData, state.sessionFactoryContext.librariesScope) }
        )
    }
    return null
}
