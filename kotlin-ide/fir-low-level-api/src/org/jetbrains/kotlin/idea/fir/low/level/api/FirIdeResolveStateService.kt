/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo

internal interface FirIdeResolveStateService {
    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService =
            ServiceManager.getService(project, FirIdeResolveStateService::class.java)!!
    }

    val fallbackModificationTracker: ModificationTracker?

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl
}

private class FirModuleData(val state: FirModuleResolveStateImpl, val modificationTracker: ModificationTracker?) {
    val modificationCount: Long = modificationTracker?.modificationCount ?: Long.MIN_VALUE

    fun isOutOfDate(): Boolean {
        val currentModCount = modificationTracker?.modificationCount
        return currentModCount != null && currentModCount > modificationCount
    }
}

internal class FirIdeResolveStateServiceImpl(val project: Project) : FirIdeResolveStateService {
    private val stateCache = mutableMapOf<IdeaModuleInfo, FirModuleData>()

    private fun createResolveState(): FirModuleResolveStateImpl {
        val provider = FirProjectSessionProvider(project)
        return FirModuleResolveStateImpl(provider)
    }

    private fun createModuleData(): FirModuleData {
        val state = createResolveState()
        // We want to invalidate cache on every PSI change for now
        // This is needed for working with high level API until the proper caching is implemented
        return FirModuleData(state, fallbackModificationTracker)
    }

    // TODO: multi thread protection
    override fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        var moduleData = stateCache.getOrPut(moduleInfo) {
            createModuleData()
        }
        if (moduleData.isOutOfDate()) {
            moduleData = createModuleData()
            stateCache[moduleInfo] = moduleData
        }
        return moduleData.state
    }

    override val fallbackModificationTracker: ModificationTracker? =
        org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService.getInstance(project).modificationTracker
}