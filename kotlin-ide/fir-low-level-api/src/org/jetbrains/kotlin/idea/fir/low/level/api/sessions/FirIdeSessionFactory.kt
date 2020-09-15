/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.checkers.registerCommonCheckers
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
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
import org.jetbrains.kotlin.idea.fir.low.level.api.FirTransformerProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeFirPhaseManager
import org.jetbrains.kotlin.idea.fir.low.level.api.IdeSessionComponents
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ModuleLibrariesSearchScope
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal object FirIdeSessionFactory {
    fun createSourcesSession(
        project: Project,
        moduleInfo: ModuleSourceInfo,
        builtinsAndCloneableSession: FirIdeBuiltinsAndCloneableSession,
        firPhaseRunner: FirPhaseRunner,
        sessionInvalidator: FirSessionInvalidator,
        builtinTypes: BuiltinTypes,
        sessionsCache: MutableMap<ModuleSourceInfo, FirIdeSourcesSession>,
        isRootModule: Boolean,
    ): FirIdeSourcesSession {
        sessionsCache[moduleInfo]?.let { return it }
        val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
        val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
        val searchScope = ModuleProductionSourceScope(moduleInfo.module)
        val dependentModules = moduleInfo.dependenciesWithoutSelf()
            .filterIsInstanceTo<ModuleSourceInfo, MutableList<ModuleSourceInfo>>(mutableListOf())
        val session = FirIdeSourcesSession(moduleInfo, dependentModules, project, searchScope, firBuilder, builtinTypes)
        sessionsCache[moduleInfo] = session


        return session.apply {
            val cache = ModuleFileCacheImpl(this)
            val firPhaseManager = IdeFirPhaseManager(FirLazyDeclarationResolver(firFileBuilder), cache, sessionInvalidator)

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

            @OptIn(ExperimentalStdlibApi::class)
            register(
                FirSymbolProvider::class,
                FirModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                        JavaSymbolProvider(this@apply, project, searchScope),
                    ),
                    dependentProviders = buildList {
                        add(createLibrarySession(moduleInfo, project, builtinsAndCloneableSession, builtinTypes).firSymbolProvider)
                        dependentModules
                            .mapTo(this) {
                                createSourcesSession(
                                    project,
                                    it,
                                    builtinsAndCloneableSession,
                                    firPhaseRunner,
                                    sessionInvalidator,
                                    builtinTypes,
                                    sessionsCache,
                                    isRootModule = false
                                ).firSymbolProvider
                            }
                    }
                )
            )

            registerJavaSpecificComponents()
            FirSessionFactory.FirSessionConfigurator(this).apply {
                if (isRootModule) {
                    registerCommonCheckers()
                }
            }.configure()
        }
    }

    fun createLibrarySession(
        moduleInfo: ModuleSourceInfo,
        project: Project,
        builtinsAndCloneableSession: FirIdeBuiltinsAndCloneableSession,
        builtinTypes: BuiltinTypes,
    ): FirIdeLibrariesSession {
        checkCanceled()
        val searchScope = ModuleLibrariesSearchScope(moduleInfo.module)
        val javaClassFinder = JavaClassFinderImpl().apply {
            setProjectInstance(project)
            setScope(searchScope)
        }
        val packagePartProvider = IDEPackagePartProvider(searchScope)

        val kotlinClassFinder = VirtualFileFinderFactory.getInstance(project).create(searchScope)
        return FirIdeLibrariesSession(moduleInfo, project, searchScope, builtinTypes).apply {
            registerCommonComponents()
            registerJavaSpecificComponents()
            registerIdeComponents()

            val javaSymbolProvider = JavaSymbolProvider(this, project, searchScope)

            val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)

            @OptIn(ExperimentalStdlibApi::class)
            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    buildList {
                        add(
                            KotlinDeserializedJvmSymbolsProvider(
                                this@apply,
                                project,
                                packagePartProvider,
                                javaSymbolProvider,
                                kotlinClassFinder,
                                javaClassFinder,
                                kotlinScopeProvider
                            )
                        )
                        add(javaSymbolProvider)
                        addAll((builtinsAndCloneableSession.firSymbolProvider as FirCompositeSymbolProvider).providers)
                    }
                )
            )
        }
    }

    fun createBuiltinsAndCloneableSession(project: Project, builtinTypes: BuiltinTypes): FirIdeBuiltinsAndCloneableSession {
        return FirIdeBuiltinsAndCloneableSession(project, builtinTypes).apply {
            registerCommonComponents()

            val kotlinScopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
            register(
                FirSymbolProvider::class,
                FirCompositeSymbolProvider(
                    this,
                    listOf(
                        FirBuiltinSymbolProvider(this, kotlinScopeProvider),
                        FirCloneableSymbolProvider(this, kotlinScopeProvider),
                    )
                )
            )
        }
    }

    private fun FirIdeSession.registerIdeComponents() {
        register(IdeSessionComponents::class, IdeSessionComponents.create(this))
    }
}