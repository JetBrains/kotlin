package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
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
context(KtAnalysisSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KtFunctionSymbol.baseMethod: KtFunctionSymbol
    get() {
        val overriddenSymbols = getAllOverriddenSymbols().filter { symbol -> symbol.isVisibleInObjC() }
        return if (overriddenSymbols.isEmpty()) this
        else overriddenSymbols.last() as KtFunctionSymbol
    }
