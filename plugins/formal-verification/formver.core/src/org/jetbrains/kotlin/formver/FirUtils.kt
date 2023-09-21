/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

val FirElement.calleeSymbol: FirBasedSymbol<*>
    get() = calleeReference?.toResolvedBaseSymbol()!!
val FirElement.calleeCallableSymbol: FirCallableSymbol<*>
    get() = calleeReference?.toResolvedCallableSymbol()!!
val FirFunctionCall.functionCallArguments: List<FirExpression>
    get() = listOfNotNull(dispatchReceiver) + argumentList.arguments
val FirFunctionSymbol<*>.effects: List<FirEffectDeclaration>
    get() = this.resolvedContractDescription?.effects ?: emptyList()
