/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.diagnosticFactoriesStorage
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceModuleData
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSyntheticFunctionInterfaceProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirEmptySymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirLibrarySessionProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.syntheticFunctionInterfacesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.flattenAndFilterOwnProviders
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory.registerLibrarySessionComponents
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.scripting.compiler.plugin.FirScriptingCompilerExtensionRegistrar
import java.io.File
import kotlin.script.experimental.host.ScriptingHostConfiguration

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
            incrementalProvider = null,
            dependencyProviders = providers,
            sharedProvider = sharedLibrarySession.symbolProvider,
        )
    )

    val providersWithShared = providers + sharedLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

    val symbolProvider = FirCachingCompositeSymbolProvider(this, providersWithShared)
    register(FirSymbolProvider::class, symbolProvider)
    register(FirProvider::class, FirLibrarySessionProvider(symbolProvider))
}

/**
 * Creates a source session for resolving the file annotations of a script, reusing the dependency providers of the [contextSession]
 * as is. The [contextSession] could be of any kind, including the Analysis API one.
 */
@OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
internal fun createScriptAnnotationResolutionSession(
    contextSession: FirSession,
    hostConfiguration: ScriptingHostConfiguration,
): FirSession {
    val configuration = CompilerConfiguration.create().apply {
        languageVersionSettings = contextSession.languageVersionSettings
        scriptingHostConfiguration = hostConfiguration
    }
    val moduleData = FirSourceModuleData(
        Name.special("<script-annotations>"),
        dependencies = emptyList(),
        dependsOnDependencies = emptyList(),
        friendDependencies = emptyList(),
        contextSession.moduleData.platform,
    )
    return FirCliSession(FirSession.Kind.Source).apply session@{
        moduleData.bindSession(this@session)
        registerModuleData(moduleData)
        registerCliCompilerAndCommonComponents(configuration.languageVersionSettings, isMetadataCompilation = false)
        registerResolveComponents(
            configuration.diagnosticFactoriesStorage ?: error("diagnosticFactoriesStorage is not registered in the configuration")
        )
        registerCliCompilerOnlyResolveComponents()
        registerJavaComponents(NoJavaModulesResolver)

        val kotlinScopeProvider = FirKotlinScopeProvider(::wrapScopeWithJvmMapped)
        register(FirKotlinScopeProvider::class, kotlinScopeProvider)
        val firProvider = FirProviderImpl(this@session, kotlinScopeProvider)
        register(FirProvider::class, firProvider)

        FirSessionConfigurator(this).apply {
            // required for applying the default imports of the script configuration
            registerExtensions(FirScriptingCompilerExtensionRegistrar(configuration).configure())
        }.configure()
        registerCommonComponentsAfterExtensionsAreConfigured()

        val dependenciesProvider = contextSession.dependenciesSymbolProvider
        register(
            StructuredProviders::class,
            StructuredProviders(
                sourceProviders = listOf(firProvider.symbolProvider),
                incrementalProvider = null,
                dependencyProviders = listOf(dependenciesProvider),
                sharedProvider = FirEmptySymbolProvider(this@session),
            )
        )
        // not the CLI caching composite: the reused dependency providers are caching themselves, and the ones of the Analysis API are not
        // obliged to compute the symbol names, as the CLI composite requires
        register(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(this@session, listOf(firProvider.symbolProvider, dependenciesProvider))
        )
        register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependenciesProvider)
    }
}

internal object NoJavaModulesResolver : JavaModuleResolver {
    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? = null

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? = null
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
    val [libModuleData, _] = state.moduleDataProvider.addNewLibraryModuleDataIfNeeded(classpath.map(File::toPath))
    if (libModuleData != null) {
        val projectEnvironment = state.sessionFactoryContext.projectEnvironment
        val libraryClasspath = state.moduleDataProvider.getModuleDataPaths(libModuleData)
            ?.let { paths -> JvmClasspath.Roots(paths.map(JvmClasspathRootId::of)) }
            ?: state.sessionFactoryContext.librariesClasspath

        return createScriptingAdditionalLibrariesSession(
            libModuleData,
            state.sessionFactoryContext,
            state.moduleDataProvider,
            state.sharedLibrarySession,
            state.extensionRegistrars,
            compilerConfiguration,
            getKotlinClassFinder = { projectEnvironment.getKotlinClassFinder(libraryClasspath) },
            getJavaFacade = {
                state.sessionFactoryContext.javaInterop
                    .createBinaryJavaFacade(it, libModuleData, state.sessionFactoryContext.librariesClasspath)
            }
        )
    }
    return null
}
