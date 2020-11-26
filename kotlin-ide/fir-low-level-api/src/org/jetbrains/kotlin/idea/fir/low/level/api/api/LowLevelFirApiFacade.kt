/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirIdeResolveStateService
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import kotlin.reflect.KClass

fun KtElement.getResolveState(): FirModuleResolveState =
    getModuleInfo().getResolveState()

fun IdeaModuleInfo.getResolveState(): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project!!).getResolveState(this)

fun KtFile.getFirFile(resolveState: FirModuleResolveState) =
    resolveState.getFirFile(this)

/**
 * Creates [FirDeclaration] by [KtDeclaration] and runs an [action] with it
 * [this@withFirDeclaration]
 * [FirDeclaration] passed to [action] should not be leaked outside [action] lambda
 * [FirDeclaration] passed to [action] will be resolved at least to [phase]
 * Otherwise, some threading problems may arise,
 *
 * [this@withFirDeclaration] should be non-local declaration (should have fully qualified name)
 */
@OptIn(InternalForInline::class)
inline fun <R> KtDeclaration.withFirDeclaration(
    resolveState: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    action: (FirDeclaration) -> R
): R {
    val firDeclaration = resolveState.findSourceFirDeclaration(this)
    firDeclaration.resolvedFirToPhase(phase, resolveState)
    return action(firDeclaration)
}

@OptIn(InternalForInline::class)
inline fun <reified F : FirDeclaration, R> KtDeclaration.withFirDeclarationOfType(
    resolveState: FirModuleResolveState,
    action: (F) -> R
): R {
    val firDeclaration = resolveState.findSourceFirDeclaration(this)
    if (firDeclaration !is F) throw InvalidFirElementTypeException(this, F::class, firDeclaration::class)
    return action(firDeclaration)
}

@OptIn(InternalForInline::class)
inline fun <reified F : FirDeclaration, R> KtLambdaExpression.withFirDeclarationOfType(
    resolveState: FirModuleResolveState,
    action: (F) -> R
): R {
    val firDeclaration = resolveState.findSourceFirDeclaration(this)
    if (firDeclaration !is F) throw InvalidFirElementTypeException(this, F::class, firDeclaration::class)
    return action(firDeclaration)
}

fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> =
    resolveState.getDiagnostics(element)

fun KtFile.collectDiagnosticsForFile(resolveState: FirModuleResolveState): Collection<Diagnostic> =
    resolveState.collectDiagnosticsForFile(this)

fun <D : FirDeclaration> D.resolvedFirToPhase(
    phase: FirResolvePhase,
    resolveState: FirModuleResolveState
): D =
    resolveState.resolvedFirToPhase(this, phase)


fun KtElement.getOrBuildFir(
    resolveState: FirModuleResolveState,
) = resolveState.getOrBuildFirFor(this)

inline fun <reified E : FirElement> KtElement.getOrBuildFirSafe(
    resolveState: FirModuleResolveState,
) = getOrBuildFir(resolveState) as? E

inline fun <reified E : FirElement> KtElement.getOrBuildFirOfType(
    resolveState: FirModuleResolveState,
): E {
    val fir = this.getOrBuildFir(resolveState)
    if (fir is E) return fir
    throw InvalidFirElementTypeException(this, E::class, fir::class)
}

class InvalidFirElementTypeException(
    ktElement: KtElement,
    expectedFirClass: KClass<out FirElement>,
    actualFirClass: KClass<out FirElement>
) : IllegalStateException("For $ktElement with text `${ktElement.text}` the $expectedFirClass expected, but $actualFirClass found")
