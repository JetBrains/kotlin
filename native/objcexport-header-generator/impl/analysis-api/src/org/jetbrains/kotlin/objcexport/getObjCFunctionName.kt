package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val resolvedObjCNameAnnotation = resolveObjCNameAnnotation()
    return ObjCExportFunctionName(
        swiftName = getSwiftName(resolvedObjCNameAnnotation),
        objCName = getObjCName(resolvedObjCNameAnnotation)
    )
}

private fun KtFunctionLikeSymbol.getObjCName(resolvedNameAnnotation: KtResolvedObjCNameAnnotation?): String {
    return resolvedNameAnnotation?.objCName ?: translationName
}

private fun KtFunctionLikeSymbol.getSwiftName(resolvedNameAnnotation: KtResolvedObjCNameAnnotation?): String {
    return resolvedNameAnnotation?.swiftName ?: translationName
}

private val KtFunctionLikeSymbol.translationName: String
    get() = when (this) {
        is KtFunctionSymbol -> name.asString()
        is KtConstructorSymbol -> "init"
        is KtPropertyGetterSymbol -> ""
        is KtAnonymousFunctionSymbol -> ""
        is KtPropertySetterSymbol -> ""
        is KtSamConstructorSymbol -> ""
    }
