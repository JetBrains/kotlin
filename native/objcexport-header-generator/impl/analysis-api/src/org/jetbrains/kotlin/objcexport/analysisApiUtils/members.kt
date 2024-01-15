package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*

context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.getAllMembers(): List<KtSymbol> {
    return getMemberScope()
        .getAllSymbols()
        .sortedBy { sortMembers(it) }
        .filterIsInstance<KtCallableSymbol>()
        .filter { member -> member.isVisibleInObjC() }
        .toList()
}

/**
 * Returns members explicitly defined in the symbol. All super methods are excluded.
 *
 * Also handles edge case with covariant method overwrite:
 *
 * ```
 * interface A {
 *   fun hello(): Any
 * }
 *
 * interface B: A {
 *   override fun hello(): String
 * }
 *
 * B.getBaseMembers().isEmpty() == true
 * ```
 *
 * More context is around [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.isBaseMethod]
 */
context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.getDeclaredMembers(): List<KtSymbol> {
    return getDeclaredMemberScope()
        .getAllSymbols()
        .sortedBy { sortMembers(it) }
        .filterIsInstance<KtCallableSymbol>()
        .filter { member ->
            member.getAllOverriddenSymbols().isEmpty() && member.isVisibleInObjC()
        }
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
