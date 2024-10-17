/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge

/**
 * Getter needs to be defined only for properties with [reservedPropertyNames]
 * ```c
 * @interface Foo
 * @property (getter=bool, setter=setBool) BOOL bool_
 * @end
 * ```
 */
internal fun ObjCExportContext.getObjCPropertyGetter(symbol: KaPropertySymbol, objCName: String): String? {

    if (!symbol.hasReservedName) return null

    val symbolGetter = symbol.getter
    val getterBridge = if (symbolGetter == null) error("KtPropertySymbol.getter is undefined") else getFunctionMethodBridge(symbolGetter)
    val getterSelector = getSelector(symbolGetter, getterBridge)

    return if (getterSelector != objCName && getterSelector.isNotBlank()) getterSelector else null
}