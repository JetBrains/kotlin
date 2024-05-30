/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.embeddings.SourceRole
import org.jetbrains.kotlin.formver.viper.ast.Position

// val FirElement.calleeSymbol: FirBasedSymbol<*>
//     get() = toReference()?.toResolvedBaseSymbol()!!
// val FirElement.calleeCallableSymbol: FirCallableSymbol<*>
//     get() = calleeReference?.toResolvedCallableSymbol()!!
@OptIn(SymbolInternals::class)
val FirPropertySymbol.isCustom: Boolean
    get() {
        val getter = getterSymbol?.fir
        val setter = setterSymbol?.fir
        return if (isVal) getter !is FirDefaultPropertyGetter
        else getter !is FirDefaultPropertyGetter || setter !is FirDefaultPropertySetter
    }

val FirFunctionCall.functionCallArguments: List<FirExpression>
    get() = listOfNotNull(dispatchReceiver) + argumentList.arguments
val FirFunctionSymbol<*>.effects: List<FirEffectDeclaration>
    get() = this.resolvedContractDescription?.effects ?: emptyList()
val KtSourceElement?.asPosition: Position
    get() = when (this) {
        null -> Position.NoPosition
        else -> Position.Wrapped(this)
    }
val FirBasedSymbol<*>.asSourceRole: SourceRole
    get() = SourceRole.FirSymbolHolder(this)