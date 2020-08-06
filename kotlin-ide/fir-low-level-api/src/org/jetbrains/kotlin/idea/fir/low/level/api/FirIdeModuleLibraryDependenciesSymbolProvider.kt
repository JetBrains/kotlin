/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirDependenciesSymbolProviderImpl
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.isLibraryClasses
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.NotThreadSafe


@NotThreadSafe
//todo make thread safe
internal class FirIdeModuleLibraryDependenciesSymbolProvider(
    session: FirIdeJavaModuleBasedSession
) : FirDependenciesSymbolProviderImpl(session) {
    override val dependencyProviders: List<FirSymbolProvider> by lazy {
        val moduleInfo = session.moduleInfo
        val sessionProvider = session.sessionProvider ?: error("No session provider found")
        val project = sessionProvider.project

        val dependencies = moduleInfo.dependenciesWithoutSelf()
            .filterIsInstance<IdeaModuleInfo>()
            .mapNotNull { dependencyInfo ->
                val dependencySession = if (dependencyInfo.isLibraryClasses()) {
                    val dependencyScope = dependencyInfo.contentScope()
                    FirLibrarySession.create(
                        dependencyInfo, sessionProvider as FirProjectSessionProvider,
                        dependencyScope, project, IDEPackagePartProvider(dependencyScope)
                    )
                } else null
                dependencySession?.firSymbolProvider
            }.toList()
        dependencies
    }
}

