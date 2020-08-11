/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.PossiblyFirFakeOverrideSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal abstract class KtFirDelegatingScope<S>(
    private val builder: KtSymbolByFirBuilder,
    final override val token: ValidityToken
) : KtScope where S : FirContainingNamesAwareScope, S : FirScope {

    abstract val firScope: S

    private val allNamesCached by cached {
        getCallableNames() + getClassLikeSymbolNames()
    }

    override fun getAllNames(): Set<Name> = allNamesCached

    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    override fun getClassLikeSymbolNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    override fun getCallableSymbols(): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getCallableNames(), builder)
    }

    override fun getClassClassLikeSymbols(): Sequence<KtClassLikeSymbol> = withValidityAssertion {
        firScope.getClassLikeSymbols(getClassLikeSymbolNames(), builder)
    }

    override fun containsName(name: Name): Boolean = withValidityAssertion {
        name in getAllNames()
    }

}

internal fun FirScope.getCallableSymbols(callableNames: Collection<Name>, builder: KtSymbolByFirBuilder) = sequence {
    callableNames.forEach { name ->
        val callables = mutableListOf<KtCallableSymbol>()
        processFunctionsByName(name) { firSymbol ->
            (firSymbol.fir as? FirSimpleFunction)?.let { fir ->
                callables.add(builder.buildFunctionSymbol(fir))
            }
        }
        processPropertiesByName(name) { firSymbol ->
            val symbol = when {
                firSymbol is FirPropertySymbol && firSymbol.isFakeOverride -> {
                    builder.buildVariableSymbol(firSymbol.fir)
                }
                else -> builder.buildCallableSymbol(firSymbol.fir)
            }
            callables.add(symbol)
        }
        yieldAll(callables)
    }
}

internal fun FirScope.getClassLikeSymbols(classLikeNames: Collection<Name>, builder: KtSymbolByFirBuilder) = sequence {
    classLikeNames.forEach { name ->
        val classLikeSymbols = mutableListOf<KtClassLikeSymbol>()
        processClassifiersByName(name) { firSymbol ->
            (firSymbol.fir as? FirClassLikeDeclaration<*>)?.let {
                classLikeSymbols.add(builder.buildClassLikeSymbol(it))
            }
        }
        yieldAll(classLikeSymbols)
    }
}