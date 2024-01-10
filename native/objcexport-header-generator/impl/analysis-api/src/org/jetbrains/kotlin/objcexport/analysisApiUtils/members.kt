package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*

context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.members(): List<KtSymbol> {
    return getMemberScope()
        .getAllSymbols()
        .sortedBy { sortMembers(it) }
        .filterIsInstance<KtCallableSymbol>()
        .filter { member -> member.isVisibleInObjC() }
        .toList()
}

/**
 * Temp workaround of [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorKt.makeMethodsOrderStable]
 */
private fun sortMembers(it: KtDeclarationSymbol) = when (it) {
    is KtConstructorSymbol -> 0
    is KtFunctionSymbol -> 1
    else -> 2
}
