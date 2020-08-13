/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.pointers

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

/**
 * `KtSymbol` is valid only during read action it was created in
 * To pass the symbol from one read action to another the KtSymbolPointer should be used
 *
 * We can restore the symbol
 *  * for symbol which came from Kotlin source it will be restored based on [com.intellij.psi.SmartPsiElementPointer]
 *  * restoring symbols which came from Java source is not supported yet
 *  * for library symbols:
 *    * for function & property symbol if its signature was not changed
 *    * for local variable symbol if code block it was declared in was not changed
 *    * for class & type alias symbols if its qualified name was not changed
 *    * for package symbol if the package is still exists
 *
 * @see org.jetbrains.kotlin.idea.frontend.api.ReadActionConfinementValidityToken
 */
abstract class KtSymbolPointer<out S : KtSymbol> {
    /**
     * @return restored symbol (possibly the new symbol instance) if one is still valid, `null` otherwise
     *
     * Consider using [org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession.restoreSymbol]
     */
    @Deprecated("Consider using org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession.restoreSymbol")
    abstract fun restoreSymbol(analysisSession: KtAnalysisSession): S?
}

inline fun <S : KtSymbol> symbolPointer(crossinline getSymbol: (KtAnalysisSession) -> S?) = object : KtSymbolPointer<S>() {
    override fun restoreSymbol(analysisSession: KtAnalysisSession): S? = getSymbol(analysisSession)
}