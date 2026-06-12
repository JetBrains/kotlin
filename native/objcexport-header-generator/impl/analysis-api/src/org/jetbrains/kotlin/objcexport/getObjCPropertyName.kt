/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.backend.konan.objCMacroDefinitions
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportPropertyName


fun ObjCExportContext.getObjCPropertyName(symbol: KaVariableSymbol): ObjCExportPropertyName {
    val resolveObjCNameAnnotation = symbol.resolveObjCNameAnnotation()
    val stringName = exportSession.overrideObjCNameOrSymbolName(symbol)

    return ObjCExportPropertyName(
        objCName = resolveObjCNameAnnotation?.objCName ?: stringName.mangleIfReservedObjCName(),
        // None of the macros we currently track need to be mangled for Swift and we do not want to break any backwards
        // compatibility.
        swiftName = resolveObjCNameAnnotation?.swiftName ?: resolveObjCNameAnnotation?.objCName ?: if (objCMacroDefinitions.contains(
                stringName
            )
        ) stringName else stringName.mangleIfReservedObjCName()
    )
}
