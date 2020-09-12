/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.deserialization.KotlinDeserializedJvmSymbolsProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseManager
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerJavaSpecificComponents
import org.jetbrains.kotlin.fir.session.registerResolveComponents
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeFirPhaseManager
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeSessionComponents
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectTransitiveDependenciesWithSelf
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object FirIdeSessionFactory {
    fun createCurrentModuleSourcesSession(
        project: Project,
        moduleInfo: ModuleSourceInfo,
        firPhaseRunner: FirPhaseRunner,
        dependentModulesSourcesSession: FirIdeDependentModulesSourcesSession,
        sessionProvider: FirIdeSessionProvider,
    ): FirIdeCurrentModuleSourcesSession {
        val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val searchScope = ModuleProductionSourceScope(moduleInfo.module)
        return FirIdeCurrentModuleSourcesSession(moduleInfo, sessionProvider, searchScope, firBuilder).apply {
            val cache = ModuleFileCacheImpl(this)
            registerCommonSourceSessionComponents(
                project,
                moduleInfo,
                scopeProvider,
                searchScope,
                cache,
                additionalSymbolProviders = listOf(
                    dependentModulesSourcesSession.firSymbolProvider,
                )
            )
            FirSessionFactory.FirSessionConfigurator(this).apply {
                registerCommonCheckers()
            }.configure()
        }
    }

    fun createDependentModulesSourcesSession(
        project: Project,
        moduleInfo: ModuleSourceInfo,
        firPhaseRunner: FirPhaseRunner,
        sessionProvider: FirIdeSessionProvider,
        librariesSession: FirIdeLibrariesSession,
    ): FirIdeDependentModulesSourcesSession {
        val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val dependentModules = moduleInfo.collectTransitiveDependenciesWithSelf()
            .filterIsInstance<ModuleSourceInfo>()
            .filterNot { it == moduleInfo }
        val searchScope = if (dependentModules.isNotEmpty()) GlobalSearchScope.union(dependentModules.map { it.contentScope() })
        else GlobalSearchScope.EMPTY_SCOPE
        return FirIdeDependentModulesSourcesSession(moduleInfo, dependentModules, sessionProvider, searchScope, firBuilder).apply {
            val cache = ModuleFileCacheImpl(this)
            registerCommonSourceSessionComponents(
                project,
                moduleInfo,
                scopeProvider,
                searchScope,
                cache,
                additionalSymbolProviders = listOf(librariesSession.firSymbolProvider)
            )
            FirSessionFactory.FirSessionConfigurator(this).configure()
        }
    }

    private fun FirIdeSourcesSession.registerCommonSourceSessionComponents(
        project: Project,
        moduleInfo: ModuleSourceInfo,
        scopeProvider: KotlinScopeProvider,
        searchScope: GlobalSearchScope,
        cache: ModuleFileCache,
        additionalSymbolProviders: List<FirSymbolProvider> = emptyList()
    ) {
        val firPhaseManager = IdeFirPhaseManager(FirLazyDeclarationResolver(firFileBuilder), cache)

        registerCommonComponents()
        registerResolveComponents()
        registerIdeComponents()

        val provider = FirIdeProvider(
            project,
            this,
            moduleInfo,
            scopeProvider,
            firFileBuilder,
            cache,
            searchScope
        )

        register(FirProvider::class, provider)
        register(FirIdeProvider::class, provider)

        register(FirPhaseManager::class, firPhaseManager)

        register(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                this,
                @OptIn(ExperimentalStdlibApi::class)
                buildList {
                    add(provider.symbolProvider)
                    add(JavaSymbolProvider(this@registerCommonSourceSessionComponents, project, searchScope))
                    addAll(additionalSymbolProviders)
                }
            ) as FirSymbolProvider
        )
        registerJavaSpecificComponents()
    }

    private fun IdeaModuleInfo.librarySearchScope(): GlobalSearchScope? = when (this) {
        is LibraryInfo -> contentScope()
        is SdkInfo -> contentScope()
        else -> null
    }

    fun createLibrarySession(
        moduleInfo: ModuleSourceInfo,
        sessionProvider: FirIdeSessionProvider,
        project: Project,
    ): FirIdeLibrariesSession {
        val dependencies = moduleInfo.collectTransitiveDependenciesWithSelf()
            .mapNotNullTo(mutableListOf()) { it.librarySearchScope() }
        val searchScope = if (dependencies.isEmpty()) GlobalSearchScope.EMPTY_SCOPE else GlobalSearchScope.union(dependencies)
        val javaClassFinder = JavaClassFinderImpl().apply {
            setProjectInstance(project)
            setScope(searchScope)
        }
        val packagePartProvider = IDEPackagePartProvider(searchScope)

        val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope)
        return FirIdeLibrariesSession(moduleInfo, sessionProvider, searchScope).apply {
            registerCommonComponents()
            registerJavaSpecificComponents()
            registerIdeComponents()

            val javaSymbolProvider = JavaSymbolProvider(this, sessionProvider.project, searchScope)

            val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(IdeSessionComponents::class, IdeSessionComponents.create(this))
            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOf(
                        KotlinDeserializedJvmSymbolsProvider(
                            this,
                            project,
                            packagePartProvider,
                            javaSymbolProvider,
                            kotlinClassFinder,
                            javaClassFinder,
                            kotlinScopeProvider
                        ),
                        FirBuiltinSymbolProvider(this, kotlinScopeProvider),
                        FirCloneableSymbolProvider(this, kotlinScopeProvider),
                        javaSymbolProvider,
                    )
                )
            )
        }
    }

    private fun FirIdeSession.registerIdeComponents() {
        register(IdeSessionComponents::class, IdeSessionComponents.create(this))
        register(FirTransformerProvider::class, FirTransformerProvider(this))
    }
}