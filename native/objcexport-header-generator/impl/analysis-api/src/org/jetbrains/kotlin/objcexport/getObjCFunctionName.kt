package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getPropertySymbol

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val annotationName =
        if (this is KtPropertyAccessorSymbol) this.getContainingSymbol()?.resolveObjCNameAnnotation()
        else resolveObjCNameAnnotation()
    return ObjCExportFunctionName(
        swiftName = getObjCFunctionName(annotationName?.swiftName),
        objCName = getObjCFunctionName(annotationName?.objCName)
    )
}

context(KtAnalysisSession)
private fun KtFunctionLikeSymbol.getObjCFunctionName(annotationName: String?): String {
    return if (annotationName != null) {
        if (this is KtPropertyAccessorSymbol) formatPropertyName(annotationName) else annotationName
    } else translationName
}

context(KtAnalysisSession)
private val KtFunctionLikeSymbol.translationName: String
    get() {
        return when (this) {
            is KtFunctionSymbol -> name.asString()
            is KtConstructorSymbol -> "init"
            is KtPropertyAccessorSymbol -> formatPropertyName()
            is KtAnonymousFunctionSymbol -> ""
            is KtSamConstructorSymbol -> ""
        }
    }

context(KtAnalysisSession)
private fun KtPropertyAccessorSymbol.formatPropertyName(annotationName: String? = null): String {
    val propertySymbol = this.getPropertySymbol()
    val name = annotationName ?: propertySymbol.name.asString()
    return when (this) {
        is KtPropertyGetterSymbol -> name
        is KtPropertySetterSymbol -> "set" + name.replaceFirstChar(kotlin.Char::uppercaseChar)
        else -> ""
    }
}