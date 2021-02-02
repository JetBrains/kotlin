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
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
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

    private fun FirTypeScope.processCallableByName(declaration: FirDeclaration) = when (declaration) {
        is FirSimpleFunction -> processFunctionsByName(declaration.name) { }
        is FirProperty -> processPropertiesByName(declaration.name) { }
        else -> error { "Invalid FIR symbol to process: ${declaration::class}" }
    }

    private fun FirTypeScope.processOverriddenDeclarations(
        declaration: FirDeclaration,
        processor: (FirCallableDeclaration<*>) -> ProcessorAction
    ) = when (declaration) {
        is FirSimpleFunction -> processOverriddenFunctions(declaration.symbol) { processor.invoke(it.fir) }
        is FirProperty -> processOverriddenProperties(declaration.symbol) { processor.invoke(it.fir) }
        else -> error { "Invalid FIR symbol to process: ${declaration::class}" }
    }

    override fun <T : KtSymbol> getOverriddenSymbols(
        callableSymbol: T,
        containingDeclaration: KtClassOrObjectSymbol
    ): List<KtCallableSymbol> {

        check(callableSymbol is KtFirSymbol<*>)
        check(containingDeclaration is KtFirClassOrObjectSymbol)

        return containingDeclaration.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { firContainer ->
            callableSymbol.firRef.withFirUnsafe { firCallableElement ->
                val firTypeScope = firContainer.unsubstitutedScope(
                    firContainer.session,
                    ScopeSession(),
                    withForcedTypeCalculator = true,
                )

                val overriddenElement = mutableSetOf<KtCallableSymbol>()
                firTypeScope.processCallableByName(firCallableElement)
                firTypeScope.processOverriddenDeclarations(firCallableElement) { overriddenDeclaration ->
                    val ktSymbol = analysisSession.firSymbolBuilder.buildCallableSymbol(overriddenDeclaration)
                    overriddenElement.add(ktSymbol)
                    ProcessorAction.NEXT
                }

                overriddenElement.toList()
            }
        }
    }

    override fun <T : KtSymbol> getOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol> = with(analysisSession) {
        val containingDeclaration = (callableSymbol as? KtSymbolWithKind)?.getContainingSymbol() as? KtClassOrObjectSymbol ?: return emptyList()
        getOverriddenSymbols(callableSymbol, containingDeclaration)
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
