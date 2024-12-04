/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun ObjCExportContext.translateToObjCExportStub(symbol: KaCallableSymbol): List<ObjCExportStub> {
    val result = mutableListOf<ObjCExportStub>()
    when (symbol) {
        is KaPropertySymbol -> {
            if (analysisSession.isObjCProperty(symbol)) {
                result.addIfNotNull(translateToObjCProperty(symbol))
            } else {
                symbol.getter?.let { getter ->
                    result.addIfNotNull(translateToObjCMethod(getter))
                }

                symbol.setter?.let { setter ->
                    result.addIfNotNull(translateToObjCMethod(setter))
                }
            }
        }
        is KaNamedFunctionSymbol -> result.addIfNotNull(translateToObjCMethod(symbol))
        else -> Unit
    }
    return result
}

internal fun ObjCExportContext.translateToObjCExportStub(symbol: KaClassSymbol): ObjCClass? = when (symbol.classKind) {
    KaClassKind.INTERFACE -> translateToObjCProtocol(symbol)
    KaClassKind.CLASS -> translateToObjCClass(symbol)
    KaClassKind.OBJECT -> translateToObjCObject(symbol)
    KaClassKind.ENUM_CLASS -> translateToObjCClass(symbol)
    KaClassKind.COMPANION_OBJECT -> translateToObjCObject(symbol)
    else -> null
}
