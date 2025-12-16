/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaStandardTypeClassIds
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol

/**
 * True if this symbol is (transitively) overriding any method of kotlin.Any
 */
internal fun KaSession.overridesAnyMethod(function: KaFunctionSymbol): Boolean {
    return function.allOverriddenSymbols.any { overridden ->
        (overridden.containingSymbol as? KaClassSymbol)?.classId == KaStandardTypeClassIds.ANY
    }
}