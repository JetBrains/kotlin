/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.sir.SirAttribute

@OptIn(KaExperimentalApi::class)
internal fun KaSession.createAvailableAttributeIfNeeded(symbol: KaDeclarationSymbol): SirAttribute.Available? {
    val deprecationLevel = symbol.deprecationStatus?.deprecationLevel ?: return null
    return when (deprecationLevel) {
        DeprecationLevelValue.WARNING -> SirAttribute.Available(message = "Deprecated in Kotlin", deprecated = true, obsoleted = "1.0")
        DeprecationLevelValue.ERROR -> SirAttribute.Available(message = "Removed in Kotlin", deprecated = true, obsoleted = "1.0")
        DeprecationLevelValue.HIDDEN -> SirAttribute.Available(message = "Removed in Kotlin", deprecated = true, obsoleted = "1.0")
    }
}