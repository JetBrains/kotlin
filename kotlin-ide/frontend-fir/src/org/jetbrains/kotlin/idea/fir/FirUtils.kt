/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.reflect.KClass




fun FirFunctionCall.isImplicitFunctionCall(): Boolean {
    if (dispatchReceiver !is FirQualifiedAccessExpression) return false
    return calleeReference.getCandidateSymbols().any(FirBasedSymbol<*>::isInvokeFunction)
}

private fun FirBasedSymbol<*>.isInvokeFunction() =
    (this as? FirNamedFunctionSymbol)?.fir?.name == OperatorNameConventions.INVOKE

fun FirFunctionCall.getCalleeSymbol(): FirBasedSymbol<*>? =
    calleeReference.getResolvedSymbolOfNameReference()

fun FirReference.getResolvedSymbolOfNameReference(): FirBasedSymbol<*>? =
    (this as? FirResolvedNamedReference)?.resolvedSymbol

internal fun FirReference.getResolvedKtSymbolOfNameReference(builder: KtSymbolByFirBuilder): KtSymbol? =
    (getResolvedSymbolOfNameReference()?.fir as? FirDeclaration)?.let { firDeclaration ->
        builder.buildSymbol(firDeclaration)
    }

internal fun FirErrorNamedReference.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    when (val diagnostic = diagnostic) {
        is ConeInapplicableCandidateError -> listOf(diagnostic.candidate.symbol)
        is ConeHiddenCandidateError -> listOf(diagnostic.candidateSymbol)
        is ConeAmbiguityError -> diagnostic.candidates
        is ConeOperatorAmbiguityError -> diagnostic.candidates
        is ConeUnsupportedCallableReferenceTarget -> listOf(diagnostic.fir.symbol)
        else -> emptyList()
    }

internal fun FirNamedReference.getCandidateSymbols(): Collection<FirBasedSymbol<*>> = when(this) {
    is FirResolvedNamedReference -> listOf(resolvedSymbol)
    is FirErrorNamedReference -> getCandidateSymbols()
    else -> emptyList()
}
