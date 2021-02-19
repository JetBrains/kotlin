/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

interface KtSymbolsMixIn : KtAnalysisSessionMixIn {
    @Suppress("DEPRECATION")
    fun <S : KtSymbol> KtSymbolPointer<S>.restoreSymbol(): S? = restoreSymbol(analysisSession)
}