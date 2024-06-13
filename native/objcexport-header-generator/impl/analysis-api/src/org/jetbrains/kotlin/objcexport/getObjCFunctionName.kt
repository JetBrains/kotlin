/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getPropertySymbol

context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun KaFunctionSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val annotationName =
        if (this is KaPropertyAccessorSymbol) containingSymbol?.resolveObjCNameAnnotation()
        else resolveObjCNameAnnotation()
    return ObjCExportFunctionName(
        swiftName = getObjCFunctionName(annotationName?.swiftName),
        objCName = getObjCFunctionName(annotationName?.objCName)
    )
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaFunctionSymbol.getObjCFunctionName(annotationName: String?): String {
    return if (annotationName != null) {
        if (this is KaPropertyAccessorSymbol) formatPropertyName(annotationName) else annotationName
    } else translationName
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private val KaFunctionSymbol.translationName: String
    get() {
        return when (this) {
            is KaNamedFunctionSymbol -> name.asString()
            is KaConstructorSymbol -> "init"
            is KaPropertyAccessorSymbol -> formatPropertyName()
            is KaAnonymousFunctionSymbol -> ""
            is KaSamConstructorSymbol -> ""
        }
    }

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaPropertyAccessorSymbol.formatPropertyName(annotationName: String? = null): String {
    val propertySymbol = this.getPropertySymbol()
    val name = annotationName ?: propertySymbol.name.asString()
    return when (this) {
        is KaPropertyGetterSymbol -> name
        is KaPropertySetterSymbol -> "set" + name.replaceFirstChar(kotlin.Char::uppercaseChar)
        else -> ""
    }
}
