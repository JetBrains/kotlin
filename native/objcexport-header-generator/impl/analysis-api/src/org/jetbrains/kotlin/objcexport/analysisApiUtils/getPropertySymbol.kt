/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.tooling.core.linearClosure

@OptIn(KaExperimentalApi::class)
internal fun KaSession.getPropertySymbol(symbol: KaPropertyAccessorSymbol): KaPropertySymbol {
    return symbol.linearClosure<KaSymbol> { it.containingDeclaration }.filterIsInstance<KaPropertySymbol>().firstOrNull()
        ?: error("Missing '${KaPropertySymbol::class} on ${symbol.render()}")
}
