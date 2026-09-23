/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.backend.konan.mangleIfStdMacro
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportPropertyName


fun ObjCExportContext.getObjCPropertyName(symbol: KaVariableSymbol): ObjCExportPropertyName =
    getObjCPropertyName(symbol, mangleObjCName = true) { it }

fun ObjCExportContext.getObjCPropertyName(
    symbol: KaVariableSymbol,
    mangleObjCName: Boolean,
    transform: (String) -> String,
): ObjCExportPropertyName {
    val resolveObjCNameAnnotation = symbol.resolveObjCNameAnnotation()
    val stringName = exportSession.overrideObjCNameOrSymbolName(symbol)
    val propertyName = transform(stringName.mangleIfReservedObjCName())
    val objCName = resolveObjCNameAnnotation?.objCName ?: propertyName

    return ObjCExportPropertyName(
        objCName = if (mangleObjCName) objCName.mangleIfStdMacro() else objCName,
        swiftName = resolveObjCNameAnnotation?.swiftName ?: resolveObjCNameAnnotation?.objCName ?: propertyName
    )
}
