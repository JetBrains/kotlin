/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeResolveStateService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        getResolveStateFor(element.getModuleInfo())

    fun getResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveState =
        FirIdeResolveStateService.getInstance(moduleInfo.project!!).getResolveState(moduleInfo)

    fun getSessionFor(element: KtElement): FirSession =
        getResolveStateFor(element).getSessionFor(element.getModuleInfo())

    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState): FirElement =
        resolveState.getOrBuildFirFor(element)

    fun getFirFile(ktFile: KtFile, resolveState: FirModuleResolveState) =
        resolveState.getFirFile(ktFile)

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> =
        resolveState.getDiagnostics(element)

    fun collectDiagnosticsForFile(ktFile: KtFile, resolveState: FirModuleResolveState): Collection<Diagnostic> =
        resolveState.collectDiagnosticsForFile(ktFile)

    fun <D : FirDeclaration> resolvedFirToPhase(
        firDeclaration: D,
        phase: FirResolvePhase,
        resolveState: FirModuleResolveState
    ): D =
        resolveState.resolvedFirToPhase(firDeclaration, phase)

}
