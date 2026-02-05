/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.isInstance
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getBridgeReceiverType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

fun ObjCExportContext.translateToObjCProperty(symbol: KaPropertySymbol): ObjCProperty? {
    if (!analysisSession.isVisibleInObjC(symbol)) return null
    return buildProperty(symbol)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildProperty]
 */
fun ObjCExportContext.buildProperty(symbol: KaPropertySymbol): ObjCProperty {
    val propertyName = getObjCPropertyName(symbol)
    val objCName = propertyName.objCName
    val symbolGetter = symbol.getter
    val getterBridge = if (symbolGetter == null) error("KtPropertySymbol.getter is undefined") else getFunctionMethodBridge(symbolGetter)
    val type = mapReturnType(symbolGetter, getterBridge.returnBridge)
    val attributes = mutableListOf<String>()
    val declarationAttributes = mutableListOf<String>()
    val privateAttribute = symbol.getSwiftPrivateAttribute()
    if (privateAttribute != null) {
        declarationAttributes.add(privateAttribute)
    }
    if (propertyName.needsSwiftNameAttribute()) {
        declarationAttributes.add(swiftNameAttribute(propertyName.swiftName))
    }
    if (!analysisSession.getBridgeReceiverType(symbol).isInstance) attributes += "class"

    if (symbol.setter == null || !analysisSession.isVisibleInObjC(symbol.setter)) {
        attributes += "readonly"
    }

    declarationAttributes.addIfNotNull(analysisSession.getObjCDeprecationStatus(symbol))

    return ObjCProperty(
        name = objCName,
        comment = analysisSession.translateToObjCComment(symbol.annotations),
        origin = analysisSession.getObjCExportStubOrigin(symbol),
        type = type,
        propertyAttributes = attributes,
        setterName = getObjCPropertySetter(symbol, objCName),
        getterName = getObjCPropertyGetter(symbol, objCName),
        declarationAttributes = declarationAttributes.toList()
    )
}

internal val String.asSetterSelector: String
    get() = "set" + replaceFirstChar(Char::uppercase) + ":"

internal val KaPropertySymbol.hasReservedName: Boolean
    get() = name.asString().isReservedPropertyName