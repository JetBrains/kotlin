/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

internal fun KaSession.isInner(symbol: KaSymbol): Boolean {
    return (symbol.containingSymbol as? KaNamedClassSymbol)?.isInner ?: false
}

internal fun KaSession.isInner(symbol: SirFromKtSymbol<*>): Boolean {
    return isInner(symbol.ktSymbol)
}