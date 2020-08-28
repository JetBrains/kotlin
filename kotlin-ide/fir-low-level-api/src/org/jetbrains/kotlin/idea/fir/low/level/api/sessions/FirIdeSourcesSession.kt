/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.PhasedFirFileResolver
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.session.registerCommonComponents
import org.jetbrains.kotlin.fir.session.registerJavaSpecificComponents
import org.jetbrains.kotlin.fir.session.registerResolveComponents
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.IdePhasedFirFileResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectTransitiveDependenciesWithSelf

/**
 * [org.jetbrains.kotlin.fir.FirSession] responsible for all Kotlin & Java source modules analysing module transitively depends on
 */
@OptIn(PrivateSessionConstructor::class)
internal class FirIdeSourcesSession private constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirIdeSessionProvider,
    override val scope: GlobalSearchScope,
    val firFileBuilder: FirFileBuilder,
) : FirIdeSession(moduleInfo, sessionProvider) {
    val cache get() = firIdeProvider.cache

    companion object {
        /**
         * Should be invoked only under a [moduleInfo]-based lock
         */
        @OptIn(SessionConfiguration::class)
        fun create(
            project: Project,
            moduleInfo: ModuleSourceInfo,
            firPhaseRunner: FirPhaseRunner,
            sessionProvider: FirIdeSessionProvider,
            librariesSession: FirIdeLibrariesSession,
        ): FirIdeSourcesSession {
            val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
            val firBuilder = FirFileBuilder(scopeProvider, firPhaseRunner)
            val dependentModules = moduleInfo.collectTransitiveDependenciesWithSelf().filterIsInstance<ModuleSourceInfo>()
            val searchScope = ModuleWithDependentsScope(project, dependentModules.map { it.module })
            return FirIdeSourcesSession(moduleInfo, sessionProvider, searchScope, firBuilder).apply {
                val cache = ModuleFileCacheImpl(this)
                val phasedFirFileResolver = IdePhasedFirFileResolver(FirLazyDeclarationResolver(firFileBuilder), cache)

                registerCommonComponents()
                registerResolveComponents()

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
                        this,
                        @OptIn(ExperimentalStdlibApi::class)
                        buildList {
                            add(provider.symbolProvider)
                            add(JavaSymbolProvider(this@apply, sessionProvider.project, searchScope))
                            add(librariesSession.firSymbolProvider)
                        }
                    ) as FirSymbolProvider
                )
                registerJavaSpecificComponents()
                extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
            }
        }
    }
}
