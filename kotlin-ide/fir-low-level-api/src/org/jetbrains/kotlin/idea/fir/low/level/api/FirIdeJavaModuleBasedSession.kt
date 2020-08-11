/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.registerCheckersComponent
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.calls.jvm.registerJvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.PhasedFirFileResolver
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeModuleLibraryDependenciesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectTransitiveDependenciesWithSelf


internal class FirIdeJavaModuleBasedSession private constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirSessionProvider,
    val firFileBuilder: FirFileBuilder,
) : FirModuleBasedSession(moduleInfo, sessionProvider) {
    val cache get() = firIdeProvider.cache

    companion object {
        /**
         * Should be invoked only under a [moduleInfo]-based lock
         */
        fun create(
            project: Project,
            moduleInfo: ModuleSourceInfo,
            firPhaseRunner: FirPhaseRunner,
            sessionProvider: FirSessionProvider,
            librariesSession: FirIdeModuleLibraryDependenciesSession,
        ): FirIdeJavaModuleBasedSession {
            val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
            val firBuilder = FirFileBuilder(sessionProvider as FirIdeSessionProvider, scopeProvider, firPhaseRunner)
            return FirIdeJavaModuleBasedSession(moduleInfo, sessionProvider, firBuilder).apply {
                val cache = ModuleFileCacheImpl(this)
                val phasedFirFileResolver = IdePhasedFirFileResolver(firBuilder, cache)
                val dependentModules = moduleInfo.collectTransitiveDependenciesWithSelf().filterIsInstance<ModuleSourceInfo>()
                val searchScope = ModuleWithDependentsScope(project, dependentModules.map { it.module })

                registerCommonComponents()
                registerResolveComponents()
                registerCheckersComponent()

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

                register(PhasedFirFileResolver::class, phasedFirFileResolver)

                register(
                    FirSymbolProvider::class,
                    FirCompositeSymbolProvider(
                        @OptIn(ExperimentalStdlibApi::class)
                        buildList {
                            add(provider)
                            add(JavaSymbolProvider(this@apply, sessionProvider.project, searchScope))
                            add(librariesSession.firSymbolProvider)
                        }
                    ) as FirSymbolProvider
                )
                registerJvmCallConflictResolverFactory()
                extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
            }
        }
    }
}