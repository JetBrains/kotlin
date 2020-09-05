/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeDependentModulesSourcesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeLibrariesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.idea.util.cachedValue
import org.jetbrains.kotlin.idea.util.getValue
import org.jetbrains.kotlin.idea.util.psiModificationTrackerBasedCachedValue
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(private val project: Project) {
    private val stateCache by psiModificationTrackerBasedCachedValue(project) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    private val moduleLibraryDependencyStateCache = ConcurrentHashMap<IdeaModuleInfo, CachedValue<FirIdeDependentModulesSourcesSession>>()

    private val librarySessionCache by cachedValue(project, LibraryModificationTracker.getInstance(project)) {
        ConcurrentHashMap<IdeaModuleInfo, FirIdeLibrariesSession>()
    }


    private fun createResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        require(moduleInfo is ModuleSourceInfo)
        val firPhaseRunner = FirPhaseRunner()
        val sessionProvider = FirIdeSessionProvider(project)

        val librariesSession = librarySessionCache.computeIfAbsent(moduleInfo) {
            FirIdeSessionFactory.createLibrarySession(moduleInfo, sessionProvider, project)
        }

        val dependentModulesSession = moduleLibraryDependencyStateCache.compute(moduleInfo) { _, cachedIdeaModuleInfo ->
            if (cachedIdeaModuleInfo?.hasUpToDateValue() == true) cachedIdeaModuleInfo
            else {
                CachedValuesManager.getManager(project).createCachedValue {
                    val dependentSession = FirIdeSessionFactory.createDependentModulesSourcesSession(
                        project,
                        moduleInfo,
                        firPhaseRunner,
                        sessionProvider,
                        librariesSession,
                    )
                    CachedValueProvider.Result.create(
                        dependentSession,
                        dependentSession.dependentModules.map { it.createModificationTracker() }
                            .takeUnless { it.isEmpty() }
                            ?: listOf(ModificationTracker.NEVER_CHANGED)
                    )
                }
            }
        }?.value ?: error("ModuleLibraryDependencyStateCache should be computed")


        val currentModuleSession = FirIdeSessionFactory.createCurrentModuleSourcesSession(
            project,
            moduleInfo,
            firPhaseRunner,
            dependentModulesSession,
            sessionProvider,
        )

        sessionProvider.init(currentModuleSession, dependentModulesSession, librariesSession)

        return FirModuleResolveStateImpl(
            moduleInfo,
            currentModuleSession,
            dependentModulesSession,
            librariesSession,
            sessionProvider,
            currentModuleSession.firFileBuilder,
            FirLazyDeclarationResolver(currentModuleSession.firFileBuilder),
        )
    }


    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.getOrPut(moduleInfo) { createResolveStateFor(moduleInfo) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()
    }
}
