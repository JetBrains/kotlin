/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol

/**
 * @return The **declared** super interfaces (**not the transitive closure**)
 */
context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.getDeclaredSuperInterfaceSymbols(): List<KtClassOrObjectSymbol> {
    return superTypes
        .asSequence()
        .mapNotNull { type -> type.symbol as? KtClassOrObjectSymbol }
        .filter { !it.isCloneable } // TODO: Write unit test for this
        .filter { superInterface -> superInterface.classKind == KtClassKind.INTERFACE }
        .toList()
}
