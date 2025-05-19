/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName
import org.jetbrains.kotlin.analysis.api.export.utilities.getPropertySymbol

fun ObjCExportContext.getObjCFunctionName(symbol: KaFunctionSymbol): ObjCExportFunctionName {
    val annotationName =
        if (symbol is KaPropertyAccessorSymbol) with(analysisSession) { symbol.containingDeclaration }?.resolveObjCNameAnnotation()
        else symbol.resolveObjCNameAnnotation()
    return ObjCExportFunctionName(
        swiftName = getObjCFunctionName(symbol, annotationName?.swiftName ?: annotationName?.objCName),
        objCName = getObjCFunctionName(symbol, annotationName?.objCName)
    )
}

private fun ObjCExportContext.getObjCFunctionName(symbol: KaFunctionSymbol, annotationName: String?): String {
    return if (annotationName != null) {
        if (symbol is KaPropertyAccessorSymbol) formatPropertyName(symbol, annotationName) else annotationName
    } else getTranslationName(symbol)
}

private fun ObjCExportContext.getTranslationName(symbol: KaFunctionSymbol): String {
    return when (symbol) {
        is KaNamedFunctionSymbol -> exportSession.exportSessionSymbolName(symbol)
        is KaConstructorSymbol -> "init"
        is KaPropertyAccessorSymbol -> formatPropertyName(symbol)
        is KaAnonymousFunctionSymbol -> ""
        is KaSamConstructorSymbol -> ""
    }
}

private fun ObjCExportContext.formatPropertyName(symbol: KaPropertyAccessorSymbol, annotationName: String? = null): String {
    val propertySymbol = analysisSession.getPropertySymbol(symbol)
    val name = annotationName ?: exportSession.exportSessionSymbolName(propertySymbol)
    return when (symbol) {
        is KaPropertyGetterSymbol -> name
        is KaPropertySetterSymbol -> "set" + name.replaceFirstChar(kotlin.Char::uppercaseChar)
    }
}
