/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeResolveStateService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KClass

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        getResolveStateFor(element.getModuleInfo())

    fun getResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveState =
        FirIdeResolveStateService.getInstance(moduleInfo.project!!).getResolveState(moduleInfo)

    fun getSessionFor(element: KtElement): FirSession =
        getResolveStateFor(element).getSessionFor(element.getModuleInfo())

    @Deprecated("Consider using withFirElement")
    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState): FirElement =
        resolveState.getOrBuildFirFor(element)

    @Suppress("DEPRECATION")
    inline fun <R> withFirElement(element: KtElement, resolveState: FirModuleResolveState, action: (FirElement) -> R): R =
        action(getOrBuildFirFor(element, resolveState))

    @Deprecated("Consider using withFirFile")
    fun getFirFile(ktFile: KtFile, resolveState: FirModuleResolveState) =
        resolveState.getFirFile(ktFile)

    @Suppress("DEPRECATION")
    inline fun <R> withFirFile(ktFile: KtFile, resolveState: FirModuleResolveState, action: (FirFile) -> R): R =
        action(getFirFile(ktFile, resolveState))

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

@Deprecated("Consider using withFir")
fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
) = LowLevelFirApiFacade.getOrBuildFirFor(this, resolveState)

@Deprecated("Consider using withFirSafe")
inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    resolveState: FirModuleResolveState,
) = LowLevelFirApiFacade.getOrBuildFirFor(this, resolveState) as? E

@Deprecated("Consider using withFirOfType")
inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(
    resolveState: FirModuleResolveState,
): E {
    val fir = LowLevelFirApiFacade.getOrBuildFirFor(this, resolveState)
    if (fir is E) return fir
    throw InvalidFirElementTypeException(this, E::class, fir::class)
}

inline fun <R> KtElement.withFir(
    resolveState: FirModuleResolveState,
    action: (FirElement) -> R,
) = LowLevelFirApiFacade.withFirElement(this, resolveState, action)


inline fun <R : Any, reified E : FirElement> KtElement.withFirSafe(
    resolveState: FirModuleResolveState,
    action: (E) -> R?
) = LowLevelFirApiFacade.withFirElement(this, resolveState) { element -> (element as? E)?.let(action) }


@Suppress("DEPRECATION")
inline fun <R, reified E : FirElement> KtElement.withFirOfType(
    resolveState: FirModuleResolveState,
    action: (E) -> R
): R = action(getOrBuildFirOfType(resolveState))


class InvalidFirElementTypeException(
    ktElement: KtElement,
    expectedFirClass: KClass<out FirElement>,
    actualFirClass: KClass<out FirElement>
) : IllegalStateException("For $ktElement with text `${ktElement.text}` the $expectedFirClass expected, but $actualFirClass found")
