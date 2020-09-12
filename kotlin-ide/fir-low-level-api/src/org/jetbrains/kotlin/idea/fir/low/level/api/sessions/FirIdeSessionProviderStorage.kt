/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.FirTransformerProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE

internal class FirIdeSessionProviderStorage(private val project: Project) {
    fun getSessionProvider(rootModule: ModuleSourceInfo): FirIdeSessionProvider {
        val transformerProvider = FirTransformerProvider()
        val firPhaseRunner = FirPhaseRunner(transformerProvider)
        val builtinTypes = BuiltinTypes()
        val builtinsAndCloneableSession = FirIdeSessionFactory.createBuiltinsAndCloneableSession(project, builtinTypes)
        val sessions = mutableMapOf<ModuleSourceInfo, FirIdeSourcesSession>()
        val session = executeWithoutPCE {
            FirIdeSessionFactory.createSourcesSession(
                project,
                rootModule,
                builtinsAndCloneableSession,
                firPhaseRunner,
                builtinTypes,
                sessions,
                isRootModule = true
            )
        }

        return FirIdeSessionProvider(project, session, sessions)
    }
}