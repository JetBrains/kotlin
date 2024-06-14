package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getPropertySymbol

context(KaSession, KtObjCExportSession)
fun KaFunctionLikeSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val annotationName =
        if (this is KaPropertyAccessorSymbol) containingSymbol?.resolveObjCNameAnnotation()
        else resolveObjCNameAnnotation()
    return ObjCExportFunctionName(
        swiftName = getObjCFunctionName(annotationName?.swiftName),
        objCName = getObjCFunctionName(annotationName?.objCName)
    )
}

context(KaSession)
private fun KaFunctionLikeSymbol.getObjCFunctionName(annotationName: String?): String {
    return if (annotationName != null) {
        if (this is KaPropertyAccessorSymbol) formatPropertyName(annotationName) else annotationName
    } else translationName
}

context(KaSession)
private val KaFunctionLikeSymbol.translationName: String
    get() {
        return when (this) {
            is KaFunctionSymbol -> name.asString()
            is KaConstructorSymbol -> "init"
            is KaPropertyAccessorSymbol -> formatPropertyName()
            is KaAnonymousFunctionSymbol -> ""
            is KaSamConstructorSymbol -> ""
        }
    }

context(KaSession)
private fun KaPropertyAccessorSymbol.formatPropertyName(annotationName: String? = null): String {
    val propertySymbol = this.getPropertySymbol()
    val name = annotationName ?: propertySymbol.name.asString()
    return when (this) {
        is KaPropertyGetterSymbol -> name
        is KaPropertySetterSymbol -> "set" + name.replaceFirstChar(kotlin.Char::uppercaseChar)
        else -> ""
    }
}