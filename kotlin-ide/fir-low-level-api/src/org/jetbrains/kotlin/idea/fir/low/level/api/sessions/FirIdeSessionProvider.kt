/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.isLibraryClasses
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe

@ThreadSafe
internal class FirIdeSessionProvider(
    override val project: Project,
) : FirSessionProvider {
    private lateinit var currentModuleSourcesSession: FirIdeSourcesSession
    private lateinit var dependentModulesModuleSourcesSession: FirIdeSourcesSession
    private lateinit var librariesSession: FirIdeLibrariesSession


    fun init(
        currentModuleSourcesSession: FirIdeSourcesSession,
        dependentModulesModuleSourcesSession: FirIdeSourcesSession,
        librariesSession: FirIdeLibrariesSession,
    ) {
        this.currentModuleSourcesSession = currentModuleSourcesSession
        this.dependentModulesModuleSourcesSession = dependentModulesModuleSourcesSession
        this.librariesSession = librariesSession
    }


    override fun getSession(moduleInfo: ModuleInfo): FirSession = when {
        moduleInfo is IdeaModuleInfo && moduleInfo.isLibraryClasses() -> {
            librariesSession /* TODO check if library is in libraries session scope */
        }
        moduleInfo == currentModuleSourcesSession.moduleInfo -> {
            currentModuleSourcesSession
        }
        moduleInfo is ModuleSourceInfo -> {
            currentModuleSourcesSession /* TODO check if source is in sources session scope */
        }
        else -> error("Invalid module info $moduleInfo")
    }
}
