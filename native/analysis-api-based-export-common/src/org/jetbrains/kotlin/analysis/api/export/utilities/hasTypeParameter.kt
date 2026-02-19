/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.symbol

/**
 * Currently generics (type parameters) are not supported.
 * So we verify if classifier and all its parents do not have them as well.
 */
@OptIn(KaExperimentalApi::class)
public fun KaNamedClassSymbol.hasTypeParameter(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {
    if (classKind == KaClassKind.INTERFACE) {
        if (typeParameters.isNotEmpty() || defaultType.allSupertypes.any { it.symbol?.typeParameters?.isNotEmpty() == true }) {
            return true
        }
    } else {
        if (typeParameters.isNotEmpty() || defaultType.allSupertypes.firstOrNull { (it.symbol as KaNamedClassSymbol).classKind != KaClassKind.INTERFACE }
                ?.let { it.symbol?.typeParameters?.isNotEmpty() } == true) {
            return true
        }
    }
    return false
}