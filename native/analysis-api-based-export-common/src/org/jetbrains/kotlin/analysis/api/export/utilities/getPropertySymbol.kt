/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol

context(session: KaSession)
@OptIn(KaExperimentalApi::class)
public fun getPropertySymbol(symbol: KaPropertyAccessorSymbol): KaPropertySymbol {
    return symbol.containingSymbol as? KaPropertySymbol
        ?: error("Missing '${KaPropertySymbol::class} on ${symbol.render()}")
}
