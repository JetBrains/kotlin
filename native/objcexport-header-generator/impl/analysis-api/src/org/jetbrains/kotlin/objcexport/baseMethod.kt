/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

/**
 * Very basic implementation
 *
 * It covers edge case: if something isn't visible in inheritance chain it should be skipped
 * See [org.jetbrains.kotlin.objcexport.tests.IsContainingSymbolVisible]
 * See also K1:
 * - [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.getBaseMethods]
 * - [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.isBaseMethod]
 */
internal fun KaSession.getBaseMethod(function: KaNamedFunctionSymbol): KaNamedFunctionSymbol {
    val overriddenSymbols = function.allOverriddenSymbols.filter { symbol -> isVisibleInObjC(symbol) }.toList()
    return if (overriddenSymbols.isEmpty()) function
    else overriddenSymbols.last() as KaNamedFunctionSymbol
}
