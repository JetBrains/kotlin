/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.analysis.registerCheckersComponent
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.registerCommonComponents
import org.jetbrains.kotlin.fir.registerResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.jvm.registerJvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider


class FirIdeJavaModuleBasedSession(
    moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider
) : FirModuleBasedSession(moduleInfo, sessionProvider) {
    companion object {
        fun create(
            project: Project,
            moduleInfo: ModuleInfo,
            sessionProvider: FirProjectSessionProvider,
            scope: GlobalSearchScope
        ): FirIdeJavaModuleBasedSession {
            return FirIdeJavaModuleBasedSession(moduleInfo, sessionProvider).apply {
                registerCommonComponents()
                registerResolveComponents()
                registerCheckersComponent()
                registerJvmCallConflictResolverFactory()

                val firIdeProvider = FirIdeProvider(project, scope, this, KotlinScopeProvider(::wrapScopeWithJvmMapped))

                register(FirProvider::class, firIdeProvider)
                register(FirIdeProvider::class, firIdeProvider)

                register(
                    FirSymbolProvider::class,
                    FirCompositeSymbolProvider(
                        listOf(
                            firProvider,
                            JavaSymbolProvider(this, sessionProvider.project, scope),
                            FirIdeModuleDependenciesSymbolProvider(this)
                        )
                    ) as FirSymbolProvider
                )
            }
        }
    }
}