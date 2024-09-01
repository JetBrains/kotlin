/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol

/**
 * @return The **declared** super interfaces (**not the transitive closure**)
 */
internal fun KaSession.getDeclaredSuperInterfaceSymbols(symbol: KaClassSymbol): List<KaClassSymbol> {
    return symbol.superTypes
        .asSequence()
        .mapNotNull { type -> type.symbol as? KaClassSymbol }
        .filter { !it.isCloneable } // TODO: Write unit test for this
        .filter { superInterface -> superInterface.classKind == KaClassKind.INTERFACE }
        .toList()
}
