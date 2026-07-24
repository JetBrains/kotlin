/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun ObjCExportContext.translateToObjCExportStub(symbol: KaCallableSymbol): List<ObjCExportStub> {
    val result = mutableListOf<ObjCExportStub>()
    when (symbol) {
        is KaPropertySymbol -> {
            if (analysisSession.isObjCProperty(symbol)) {
                result.addIfNotNull(translateToObjCProperty(symbol))
                if (symbol.getter != null) {
                    val selector = getSelector(
                        symbol.getter!!,
                        getFunctionMethodBridge(
                            symbol.getter!!
                        )
                    )

                    if (this.exportSession.configuration.explicitMethodFamilyName && selector.isASpecialName()) {
                        result.addIfNotNull(translateToObjCMethod(symbol.getter!!))
                    }
                }
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

internal fun ObjCExportContext.translateToObjCExportStub(symbol: KaClassSymbol): ObjCExportTranslatedClass? = when (symbol.classKind) {
    KaClassKind.INTERFACE -> ObjCExportTranslatedClass(translateToObjCProtocol(symbol))
    KaClassKind.CLASS -> translateToObjCClass(symbol)
    KaClassKind.OBJECT -> ObjCExportTranslatedClass(translateToObjCObject(symbol))
    KaClassKind.ENUM_CLASS -> translateToObjCClass(symbol)
    KaClassKind.COMPANION_OBJECT -> ObjCExportTranslatedClass(translateToObjCObject(symbol))
    else -> null
}
