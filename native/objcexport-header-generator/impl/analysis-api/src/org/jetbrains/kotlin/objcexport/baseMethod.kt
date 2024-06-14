package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
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
context(KaSession)
internal val KaFunctionSymbol.baseMethod: KaFunctionSymbol
    get() {
        val overriddenSymbols = allOverriddenSymbols.filter { symbol -> symbol.isVisibleInObjC() }.toList()
        return if (overriddenSymbols.isEmpty()) this
        else overriddenSymbols.last() as KaFunctionSymbol
    }
