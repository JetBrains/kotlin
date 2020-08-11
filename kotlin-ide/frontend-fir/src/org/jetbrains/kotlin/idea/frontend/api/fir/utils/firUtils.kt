/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

internal inline val FirDeclaration.overriddenDeclaration: FirDeclaration?
    get() {
        val symbol = (this as? FirSymbolOwner<*>)?.symbol ?: return null
        return (symbol as? FirCallableSymbol)?.overriddenSymbol?.fir as? FirDeclaration
    }