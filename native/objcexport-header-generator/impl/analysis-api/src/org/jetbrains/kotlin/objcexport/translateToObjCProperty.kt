/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objCMacroDefinitions
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.isInstance
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getBridgeReceiverType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.utils.addIfNotNull

fun ObjCExportContext.translateToObjCProperty(symbol: KaPropertySymbol): ObjCProperty? {
    if (!isVisibleInObjC(symbol, session = analysisSession)) return null
    return buildProperty(symbol)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildProperty]
 */
fun ObjCExportContext.buildProperty(symbol: KaPropertySymbol): ObjCProperty {
    val propertyName = getObjCPropertyName(symbol)
    val objCName = propertyName.objCName
    val swiftName = propertyName.swiftName
    val symbolGetter = symbol.getter
    val getterBridge = if (symbolGetter == null) error("KtPropertySymbol.getter is undefined") else getFunctionMethodBridge(symbolGetter)
    val type = mapReturnType(symbolGetter, getterBridge.returnBridge)
    val attributes = mutableListOf<String>()
    val declarationAttributes = mutableListOf(symbol.getSwiftPrivateAttribute() ?: swiftNameAttribute(swiftName))

    if (!getBridgeReceiverType(symbol, session = analysisSession).isInstance) attributes += "class"

    if (symbol.setter == null || !isVisibleInObjC(symbol.setter, session = analysisSession)) {
        attributes += "readonly"
    }

    declarationAttributes.addIfNotNull(getObjCDeprecationStatus(symbol, session = analysisSession))

    return ObjCProperty(
        name = objCName,
        comment = translateToObjCComment(symbol.annotations, session = analysisSession),
        origin = getObjCExportStubOrigin(symbol, session = analysisSession),
        type = type,
        propertyAttributes = attributes,
        setterName = getObjCPropertySetter(symbol, objCName),
        getterName = getObjCPropertyGetter(symbol, objCName),
        declarationAttributes = declarationAttributes.toList()
    )
}

internal val String.asSetterSelector: String
    get() = "set" + replaceFirstChar(Char::uppercase) + ":"

/**
 * A getter or setter is defined only when it differs from the property's name. This can happen in 4 scenarios:
 * 1. Property name is a reserved keyword such as bool: (getter=bool, setter=setBool:) BOOL bool_;
 * 2. Accessor clashes with a method (an NSObject method, for e.g.): (getter=autorelease_) int32_t autorelease;
 * 3. Property name clashes with a macro: (setter=setNULL:) int32_t NULL_;
 * 4. Accessor has to be prefixed with "do": (getter=doInit) int32_t init;
 */
internal fun KaPropertySymbol.needsAccessor(objCName: String, explicitMethodFamilyName: Boolean): Boolean {
    val name = this.name.asString()
    return name.isReservedClassOrObjectName || name in objCMacroDefinitions || objCName.isSpecialFamilyOrInit(explicitMethodFamilyName)
}
