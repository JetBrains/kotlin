/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.calls

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol

fun KtCallTarget.getSuccessCallSymbolOrNull(): KtFunctionLikeSymbol? = when (this) {
    is KtSuccessCallTarget -> symbol
    is KtErrorCallTarget -> null
}

inline fun <reified S : KtFunctionLikeSymbol> KtCall.isSuccessCallOf(predicate: (S) -> Boolean): Boolean {
    if (this !is KtFunctionCall) return false
    val symbol = targetFunction.getSuccessCallSymbolOrNull() ?: return false
    if (symbol !is S) return false
    return predicate(symbol)
}