package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName

/**
 * Currently covers only basic cases
 * KT-65774: check K1 implementation and add more tests.
 * See K1 part of implementation at [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getMangledName]
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val resolveObjCNameAnnotation = resolveObjCNameAnnotation()

    val fallbackName = when (this) {
        is KtFunctionSymbol -> name.asString()
        is KtConstructorSymbol -> "init"
        is KtPropertyGetterSymbol -> "get"
        is KtAnonymousFunctionSymbol -> ""
        is KtPropertySetterSymbol -> ""
        is KtSamConstructorSymbol -> ""
    }

    return ObjCExportFunctionName(
        swiftName = resolveObjCNameAnnotation?.swiftName ?: fallbackName,
        objCName = resolveObjCNameAnnotation?.objCName ?: fallbackName
    )
}