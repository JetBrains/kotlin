package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtConstructorSymbol.translateToObjCConstructors(): List<ObjCMethod> {
    if (!isVisibleInObjC()) return emptyList()
    val result = mutableListOf<ObjCMethod>()

    result.add(buildObjCMethod())

    result.add(
        ObjCMethod(
            null,
            null,
            false,
            ObjCInstanceType,
            listOf("new"),
            emptyList(),
            listOf(
                "availability(swift, unavailable, message=\"use object initializers instead\")"
            )
        )
    )

    return result
}