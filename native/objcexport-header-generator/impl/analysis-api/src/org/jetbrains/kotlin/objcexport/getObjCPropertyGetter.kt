/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge

internal fun ObjCExportContext.getObjCPropertyGetter(symbol: KaPropertySymbol, objCName: String): String? {
    // See KaPropertySymbol.requiresAccessor() to know when an explicit setter is required.
    if (!symbol.needsAccessor(objCName, exportSession.configuration.explicitMethodFamilyName))
        return null

    val symbolGetter = symbol.getter
    val getterBridge = if (symbolGetter == null) error("KtPropertySymbol.getter is undefined") else getFunctionMethodBridge(symbolGetter)
    val getterSelector = getSelector(symbolGetter, getterBridge, true)

    return if (getterSelector != objCName && getterSelector.isNotBlank()) getterSelector else null
}
