package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getPropertySymbol

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val resolvedObjCNameAnnotation = resolveObjCNameAnnotation()
    return ObjCExportFunctionName(
        swiftName = getSwiftName(resolvedObjCNameAnnotation),
        objCName = getObjCName(resolvedObjCNameAnnotation)
    )
}

context(KtAnalysisSession)
private fun KtFunctionLikeSymbol.getObjCName(resolvedNameAnnotation: KtResolvedObjCNameAnnotation?): String {
    return resolvedNameAnnotation?.objCName ?: translationName
}

context(KtAnalysisSession)
private fun KtFunctionLikeSymbol.getSwiftName(resolvedNameAnnotation: KtResolvedObjCNameAnnotation?): String {
    return resolvedNameAnnotation?.swiftName ?: translationName
}

context(KtAnalysisSession)
private val KtFunctionLikeSymbol.translationName: String
    get() {
        return when (this) {
            is KtFunctionSymbol -> name.asString()
            is KtConstructorSymbol -> "init"
            is KtPropertyAccessorSymbol -> this.objCPropertyName
            is KtAnonymousFunctionSymbol -> ""
            is KtSamConstructorSymbol -> ""
        }
    }

context(KtAnalysisSession)
private val KtPropertyAccessorSymbol.objCPropertyName: String
    get() {
        return if (!this.getPropertySymbol().isObjCProperty) {
            val containingSymbol = getContainingSymbol()
            when (this) {
                is KtPropertyGetterSymbol -> {
                    (containingSymbol as KtPropertySymbol).name.asString()
                }
                is KtPropertySetterSymbol -> {
                    "set" + ((containingSymbol as KtPropertySymbol).name.asString()).replaceFirstChar(kotlin.Char::uppercaseChar)
                }
                else -> ""
            }
        } else ""
    }