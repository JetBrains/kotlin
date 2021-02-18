/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverridePropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind

internal class KtFirSymbolDeclarationOverridesProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolDeclarationOverridesProvider(), KtFirAnalysisSessionComponent {

    override fun <T : KtSymbol> getAllOverriddenSymbols(
        callableSymbol: T,
    ): List<KtCallableSymbol> {
        val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
        processOverrides(callableSymbol) { firTypeScope, firCallableDeclaration ->
            firTypeScope.processAllOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
                overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(overriddenElement)
            }
        }
        return overriddenElement.map { analysisSession.firSymbolBuilder.buildCallableSymbol(it.fir) }
    }

    override fun <T : KtSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol> {
        val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
        processOverrides(callableSymbol) { firTypeScope, firCallableDeclaration ->
            firTypeScope.processDirectOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
                overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(overriddenElement)
            }
        }
        return overriddenElement.map { analysisSession.firSymbolBuilder.buildCallableSymbol(it.fir) }
    }

    private fun FirTypeScope.processCallableByName(declaration: FirDeclaration) = when (declaration) {
        is FirSimpleFunction -> processFunctionsByName(declaration.name) { }
        is FirProperty -> processPropertiesByName(declaration.name) { }
        else -> error { "Invalid FIR symbol to process: ${declaration::class}" }
    }

    private fun FirTypeScope.processAllOverriddenDeclarations(
        declaration: FirDeclaration,
        processor: (FirCallableDeclaration<*>) -> Unit
    ) = when (declaration) {
        is FirSimpleFunction -> processOverriddenFunctions(declaration.symbol) { symbol ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        is FirProperty -> processOverriddenProperties(declaration.symbol) { symbol ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        else -> error { "Invalid FIR symbol to process: ${declaration::class}" }
    }

    private fun FirTypeScope.processDirectOverriddenDeclarations(
        declaration: FirDeclaration,
        processor: (FirCallableDeclaration<*>) -> Unit
    ) = when (declaration) {
        is FirSimpleFunction -> processDirectOverriddenFunctionsWithBaseScope(declaration.symbol) { symbol, _ ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        is FirProperty -> processDirectOverriddenPropertiesWithBaseScope(declaration.symbol) { symbol, _ ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        else -> error { "Invalid FIR symbol to process: ${declaration::class}" }
    }

    private inline fun <T : KtSymbol> processOverrides(
        callableSymbol: T,
        crossinline process: (FirTypeScope, FirDeclaration) -> Unit
    ) {
        require(callableSymbol is KtFirSymbol<*>)
        val containingDeclaration = with(analysisSession) {
            (callableSymbol as? KtSymbolWithKind)?.getContainingSymbol() as? KtClassOrObjectSymbol
        } ?: return
        check(containingDeclaration is KtFirClassOrObjectSymbol)

        processOverrides(containingDeclaration, callableSymbol, process)
    }

    private inline fun processOverrides(
        containingDeclaration: KtFirClassOrObjectSymbol,
        callableSymbol: KtFirSymbol<*>,
        crossinline process: (FirTypeScope, FirDeclaration) -> Unit
    ) {
        containingDeclaration.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { firContainer ->
            callableSymbol.firRef.withFirUnsafe { firCallableDeclaration ->
                val firTypeScope = firContainer.unsubstitutedScope(
                    firContainer.session,
                    ScopeSession(),
                    withForcedTypeCalculator = true,
                )
                firTypeScope.processCallableByName(firCallableDeclaration)
                process(firTypeScope, firCallableDeclaration)
            }
        }
    }

    private fun FirCallableSymbol<*>.collectIntersectionOverridesSymbolsTo(to: MutableCollection<FirCallableSymbol<*>>) {
        when (this) {
            is FirIntersectionOverrideFunctionSymbol -> {
                intersections.forEach { it.collectIntersectionOverridesSymbolsTo(to) }
            }
            is FirIntersectionOverridePropertySymbol -> {
                intersections.forEach { it.collectIntersectionOverridesSymbolsTo(to) }
            }
            else -> {
                to += this
            }
        }
    }

    override fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): Collection<KtCallableSymbol> {
        require(symbol is KtFirSymbol<*>)
        if (symbol.origin != KtSymbolOrigin.INTERSECTION_OVERRIDE) return emptyList()
        return symbol.firRef.withFir { fir ->
            val firSymbol = (fir as? FirSymbolOwner<*>)?.symbol ?: return@withFir emptyList()
            firSymbol.getIntersectionOverriddenSymbols().map { analysisSession.firSymbolBuilder.buildCallableSymbol(it.fir) }
        }
    }

    private fun AbstractFirBasedSymbol<*>.getIntersectionOverriddenSymbols(): Collection<FirCallableSymbol<*>> {
        require(this is FirCallableSymbol<*>) {
            "Required FirCallableSymbol but ${this::class} found"
        }
        return when (this) {
            is FirIntersectionOverrideFunctionSymbol -> intersections
            is FirIntersectionOverridePropertySymbol -> intersections
            else -> listOf(this)
        }
    }
}
