/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.util.getElementTextInContext

object LowLevelFirApiFacade {
    fun getResolveStateFor(element: KtElement): FirModuleResolveState =
        element.firResolveState()

    fun getResolveStateForCompletion(element: KtElement, originalState: FirModuleResolveState): FirModuleResolveState {
        check(originalState is FirModuleResolveStateImpl)
        return FirModuleResolveStateForCompletion(originalState)
    }

    fun getSessionFor(element: KtElement): FirSession =
        getResolveStateFor(element).getSessionFor(element.getModuleInfo())

    fun getOrBuildFirFor(element: KtElement, resolveState: FirModuleResolveState, phase: FirResolvePhase): FirElement =
        resolveState.getOrBuildFirFor(element, phase)

    class FirCompletionContext internal constructor(
        val session: FirSession,
        private val towerDataContextCollector: FirTowerDataContextCollector,
        private val state: FirModuleResolveState,
    ) {
        fun getTowerDataContext(element: KtElement): FirTowerDataContext {
            var current: PsiElement? = element
            while (current is KtElement) {
                val mappedFir = state.getCachedMappingForCompletion(current)

                if (mappedFir is FirStatement) {
                    towerDataContextCollector.getContext(mappedFir)?.let { return it }
                }
                current = current.parent
            }

            error("No context for ${element.getElementTextInContext()}")
        }
    }

    fun buildCompletionContextForFunction(
        firFile: FirFile,
        element: KtNamedFunction,
        originalElement: KtNamedFunction,
        state: FirModuleResolveState,
        phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
    ): FirCompletionContext {
        val firIdeProvider = firFile.session.firIdeProvider
        val originalFunction = state.getOrBuildFirFor(originalElement, phase) as FirSimpleFunction
        val builtFunction = firIdeProvider.buildFunctionWithBody(element)
        val contextCollector = FirTowerDataContextCollector()

        // right now we can't resolve builtFunction header properly, as it built right in air,
        // without file, which is now required for running stages other then body resolve, so we
        // take original function header (which is resolved) and copy replacing body with body from builtFunction
        val frankensteinFunction = buildSimpleFunctionCopy(originalFunction) {
            body = builtFunction.body
            symbol = builtFunction.symbol as FirNamedFunctionSymbol
            resolvePhase = minOf(originalFunction.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtFunction.source
            this.session = state.firIdeSourcesSession
        }

        val function = frankensteinFunction.apply {
            state.lazyResolveFunctionForCompletion(this, firFile, firIdeProvider, phase, contextCollector)
            state.recordPsiToFirMappingsForCompletionFrom(this, firFile, element.containingKtFile)
        }

        return FirCompletionContext(
            function.session,
            contextCollector,
            state
        )
    }

    fun getDiagnosticsFor(element: KtElement, resolveState: FirModuleResolveState): Collection<Diagnostic> {
        return resolveState.getDiagnostics(element)
    }
}
