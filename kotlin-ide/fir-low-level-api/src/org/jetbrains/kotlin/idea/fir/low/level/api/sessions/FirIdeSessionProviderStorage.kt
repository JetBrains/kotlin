/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.FirTransformerProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.KotlinFirOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeSessionProviderStorage(private val project: Project) {
    private val sessionsCache = ConcurrentHashMap<ModuleSourceInfo, FromModuleViewSessionCache>()

    fun getSessionProvider(rootModule: ModuleSourceInfo): FirIdeSessionProvider {
        val transformerProvider = FirTransformerProvider()
        val firPhaseRunner = FirPhaseRunner(transformerProvider)

        val builtinTypes = BuiltinTypes()
        val builtinsAndCloneableSession = FirIdeSessionFactory.createBuiltinsAndCloneableSession(project, builtinTypes)
        val cache = sessionsCache.getOrPut(rootModule) { FromModuleViewSessionCache(rootModule) }
        val (sessions, session) = cache.withMappings { mappings ->
            val sessions = mutableMapOf<ModuleSourceInfo, FirIdeSourcesSession>().apply { putAll(mappings) }
            val session = executeWithoutPCE {
                FirIdeSessionFactory.createSourcesSession(
                    project,
                    rootModule,
                    builtinsAndCloneableSession,
                    firPhaseRunner,
                    cache.sessionInvalidator,
                    builtinTypes,
                    sessions,
                    isRootModule = true
                )
            }
            sessions to session
        }

        return FirIdeSessionProvider(project, session, sessions)
    }
}

private class FromModuleViewSessionCache(
    val root: ModuleSourceInfo,
) {
    @Volatile
    private var mappings: Map<ModuleSourceInfo, FirSessionWithModificationTracker> = emptyMap()

    val sessionInvalidator: FirSessionInvalidator = FirSessionInvalidator { session ->
        mappings[session.moduleInfo]?.invalidate()
    }


    inline fun <R> withMappings(
        action: (Map<ModuleSourceInfo, FirIdeSourcesSession>) -> Pair<Map<ModuleSourceInfo, FirIdeSourcesSession>, R>
    ): Pair<Map<ModuleSourceInfo, FirIdeSourcesSession>, R> {
        val (newMappings, result) = action(getSessions().mapValues { it.value })
        mappings = newMappings.mapValues { FirSessionWithModificationTracker(it.value) }
        return newMappings to result
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getSessions(): Map<ModuleSourceInfo, FirIdeSourcesSession> = buildMap {
        val sessions = mappings.values
        val sessionToValidity = hashMapOf<FirSessionWithModificationTracker, Boolean>()

        var isValid = true
        fun dfs(session: FirSessionWithModificationTracker) {
            sessionToValidity[session]?.let { valid ->
                if (!valid) isValid = false
                return
            }
            sessionToValidity[session] = session.isValid
            session.firSession.dependencies.forEach { dependency ->
                mappings[dependency]?.let(::dfs)
            }
            if (!session.isValid) {
                isValid = false
            }
            if (!isValid) {
                sessionToValidity[session] = false
            }
        }

        for (session in sessions) {
            if (session !in sessionToValidity) {
                isValid = true
                dfs(session)
            }
        }
        return sessionToValidity.entries
            .mapNotNull { (session, valid) -> session.takeIf { valid } }
            .associate { session -> session.firSession.moduleInfo to session.firSession }
    }
}

private class FirSessionWithModificationTracker(
    val firSession: FirIdeSourcesSession,
) {
    private val modificationTracker = firSession.project.service<KotlinFirOutOfBlockModificationTrackerFactory>()
        .createModuleOutOfBlockModificationTracker(firSession.moduleInfo.module)

    private val timeStamp = modificationTracker.modificationCount

    @Volatile
    private var isInvalidated = false

    fun invalidate() {
        isInvalidated = true
    }

    val isValid: Boolean get() = isInvalidated || modificationTracker.modificationCount == timeStamp
}