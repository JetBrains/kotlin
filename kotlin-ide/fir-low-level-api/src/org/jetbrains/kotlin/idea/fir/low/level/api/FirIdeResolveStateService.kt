/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProviderStorage
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.KotlinFirOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.util.cachedValue
import org.jetbrains.kotlin.idea.util.getValue
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(project: Project) {
    private val sessionProviderStorage = FirIdeSessionProviderStorage(project)

    private val stateCache by cachedValue(
        project,
        project.service<KotlinFirOutOfBlockModificationTrackerFactory>().createProjectWideOutOfBlockModificationTracker(),
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.computeIfAbsent(moduleInfo) { createResolveStateFor(moduleInfo, sessionProviderStorage) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()

        internal fun createResolveStateFor(
            moduleInfo: IdeaModuleInfo,
            sessionProviderStorage: FirIdeSessionProviderStorage
        ): FirModuleResolveStateImpl {
            require(moduleInfo is ModuleSourceInfo)
            val sessionProvider = sessionProviderStorage.getSessionProvider(moduleInfo)
            val firFileBuilder = sessionProvider.rootModuleSession.firFileBuilder
            return FirModuleResolveStateImpl(
                moduleInfo.project,
                moduleInfo,
                sessionProvider,
                firFileBuilder,
                FirLazyDeclarationResolver(firFileBuilder),
            )
        }
    }
}

@TestOnly
fun createResolveStateForNoCaching(
    moduleInfo: IdeaModuleInfo,
): FirModuleResolveState = FirIdeResolveStateService.createResolveStateFor(moduleInfo, FirIdeSessionProviderStorage(moduleInfo.project!!))


