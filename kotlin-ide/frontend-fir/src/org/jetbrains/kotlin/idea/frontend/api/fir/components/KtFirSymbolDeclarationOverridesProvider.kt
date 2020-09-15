/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.*

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

        return callableSymbol.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { firCallableElement ->

            containingDeclaration.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { containingDeclaration ->

                val firTypeScope = containingDeclaration.unsubstitutedScope(
                    containingDeclaration.session,
                    ScopeSession()
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
}