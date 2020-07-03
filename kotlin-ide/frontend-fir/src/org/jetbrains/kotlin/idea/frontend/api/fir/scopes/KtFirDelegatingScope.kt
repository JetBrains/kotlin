/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.PossiblyFirFakeOverrideSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal abstract class KtFirDelegatingScope(private val builder: KtSymbolByFirBuilder) : KtScope {
    abstract val firScope: FirScope

    private var allNamesCached: Set<Name>? = null

    override fun getAllNames(): Set<Name> = withValidityAssertion {
        if (allNamesCached == null) {
            allNamesCached = firScope.getCallableNames() + firScope.getClassifierNames()
        }
        allNamesCached!!
    }

    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    override fun getClassLikeSymbolNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    override fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            yieldAll(getCallableSymbols())
            yieldAll(getClassClassLikeSymbols())
        }
    }

    override fun getCallableSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            firScope.getCallableNames().forEach { name ->
                val callables = mutableListOf<KtSymbol>()
                firScope.processFunctionsByName(name) { firSymbol ->
                    (firSymbol.fir as? FirSimpleFunction)?.let { fir ->
                        callables.add(builder.buildFunctionSymbol(fir, firSymbol.realDeclarationOrigin()))
                    }
                }
                firScope.processPropertiesByName(name) { firSymbol ->
                    val symbol = when {
                        firSymbol is FirPropertySymbol && firSymbol.isFakeOverride -> {
                            builder.buildVariableSymbol(firSymbol.fir, firSymbol.realDeclarationOrigin())
                        }
                        else -> builder.buildSymbol(firSymbol.fir)
                    }
                    callables.add(symbol)
                }
                yieldAll(callables)
            }
        }
    }

    override fun getClassClassLikeSymbols(): Sequence<KtClassLikeSymbol> = withValidityAssertion {
        sequence {
            firScope.getClassifierNames().forEach { name ->
                val classLikeSymbols = mutableListOf<KtClassLikeSymbol>()
                firScope.processClassifiersByName(name) { firSymbol ->
                    (firSymbol.fir as? FirClassLikeDeclaration<*>)?.let {
                        classLikeSymbols.add(builder.buildClassLikeSymbol(it))
                    }
                }
                yieldAll(classLikeSymbols)
            }
        }
    }

    override fun containsName(name: Name): Boolean = withValidityAssertion {
        name in getAllNames()
    }

    companion object {
        private fun FirBasedSymbol<*>.realDeclarationOrigin(): FirDeclarationOrigin? {
            if (this !is PossiblyFirFakeOverrideSymbol<*, *> || !isFakeOverride) return null
            var current: FirBasedSymbol<*>? = this.overriddenSymbol
            while (current is PossiblyFirFakeOverrideSymbol<*, *> && current.isFakeOverride) {
                current = current.overriddenSymbol
            }
            return (current?.fir as? FirDeclaration)?.origin
        }
    }
}