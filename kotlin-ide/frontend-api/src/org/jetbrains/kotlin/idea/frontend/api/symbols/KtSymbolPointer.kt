/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession

/**
 * `KtSymbol` is valid only during read action it was created in
 * To pass the symbol from one read action to another the KtSymbolPointer should be used
 *
 * We can restore the symbol
 *  * for function & property symbol if its signature was not changed
 *  * for local variable symbol if code block it was declared in was not changed
 *  * for class & type alias symbols if its qualified name was not changed
 *  * for package symbol if the package is still exists
 *
 * @see org.jetbrains.kotlin.idea.frontend.api.ReadActionConfinementValidityToken
 */
interface KtSymbolPointer<out S : KtSymbol> {
    /**
     * @return restored symbol (possibly the new symbol instance) if one is still valid, `null` otherwise
     */
    fun restoreSymbol(analysisSession: KtAnalysisSession): S?
}

object NonRestorableKtSymbolPointer : KtSymbolPointer<Nothing> {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): Nothing? = null
}

inline fun <S : KtSymbol> symbolPointer(crossinline getSymbol: (KtAnalysisSession) -> S?) = object : KtSymbolPointer<S> {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): S? = getSymbol(analysisSession)
}