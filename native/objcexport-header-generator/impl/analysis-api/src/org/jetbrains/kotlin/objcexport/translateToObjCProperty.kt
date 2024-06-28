/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCIdType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.isInstance
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getBridgeReceiverType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.utils.addIfNotNull

fun ObjCExportContext.translateToObjCProperty(symbol: KaPropertySymbol): ObjCProperty? {
    if (!kaSession.isVisibleInObjC(symbol)) return null
    return buildProperty(symbol)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildProperty]
 */
fun ObjCExportContext.buildProperty(symbol: KaPropertySymbol): ObjCProperty {
    val propertyName = getObjCPropertyName(symbol)
    val name = propertyName.objCName
    val symbolGetter = symbol.getter
    val getterBridge = if (symbolGetter == null) error("KtPropertySymbol.getter is undefined") else getFunctionMethodBridge(symbolGetter)
    val type = mapReturnType(symbolGetter, getterBridge.returnBridge)
    val attributes = mutableListOf<String>()
    val setterName: String?

    if (!kaSession.getBridgeReceiverType(symbol).isInstance) attributes += "class"

    val propertySetter = symbol.setter
    // Note: the condition below is similar to "toObjCMethods" logic in [ObjCExportedInterface.createCodeSpec].
    val shouldBeSetterExposed = true //TODO: mapper.shouldBeExposed

    if (propertySetter != null && shouldBeSetterExposed) {
        val setterSelector = getSelector(propertySetter, getFunctionMethodBridge(propertySetter))
        setterName = if (setterSelector == name.asSetterSelector) null else setterSelector
    } else {
        attributes += "readonly"
        setterName = null
    }

    val getterSelector = getSelector(symbolGetter, getterBridge)
    val getterName: String? = if (getterSelector != name && getterSelector.isNotBlank() == true) getterSelector else null
    val declarationAttributes = mutableListOf(symbol.getSwiftPrivateAttribute() ?: swiftNameAttribute(propertyName.swiftName))

    declarationAttributes.addIfNotNull(kaSession.getObjCDeprecationStatus(symbol))

    return ObjCProperty(
        name = name,
        comment = kaSession.translateToObjCComment(symbol.annotations),
        origin = kaSession.getObjCExportStubOrigin(symbol),
        type = type ?: ObjCIdType, //[ObjCIdType] temp fix, should be translated properly, see KT-65709
        propertyAttributes = attributes,
        setterName = if (setterName.isNullOrBlank()) null else setterName,
        getterName = getterName,
        declarationAttributes = declarationAttributes
    )
}

private val String.asSetterSelector: String
    get() = "set" + replaceFirstChar(Char::uppercase) + ":"