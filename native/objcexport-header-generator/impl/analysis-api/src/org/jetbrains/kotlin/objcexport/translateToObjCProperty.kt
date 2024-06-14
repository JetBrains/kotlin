/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCIdType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.isInstance
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.analysisApiUtils.bridgeReceiverType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFunctionMethodBridge
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.utils.addIfNotNull

context(KaSession, KtObjCExportSession)
fun KaPropertySymbol.translateToObjCProperty(): ObjCProperty? {
    if (!isVisibleInObjC()) return null
    return buildProperty()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildProperty]
 */
context(KaSession, KtObjCExportSession)
fun KaPropertySymbol.buildProperty(): ObjCProperty {
    val propertyName = getObjCPropertyName()
    val name = propertyName.objCName
    val getterBridge = getter?.getFunctionMethodBridge() ?: error("KtPropertySymbol.getter is undefined")
    val type = getter?.mapReturnType(getterBridge.returnBridge)
    val attributes = mutableListOf<String>()
    val setterName: String?

    if (!bridgeReceiverType.isInstance) attributes += "class"

    val propertySetter = setter
    // Note: the condition below is similar to "toObjCMethods" logic in [ObjCExportedInterface.createCodeSpec].
    val shouldBeSetterExposed = true //TODO: mapper.shouldBeExposed

    if (propertySetter != null && shouldBeSetterExposed) {
        val setterSelector = propertySetter.getSelector(propertySetter.getFunctionMethodBridge())
        setterName = if (setterSelector == name.asSetterSelector) null else setterSelector
    } else {
        attributes += "readonly"
        setterName = null
    }

    val getterSelector = getter?.getSelector(getterBridge)
    val getterName: String? = if (getterSelector != name && getterSelector?.isNotBlank() == true) getterSelector else null
    val declarationAttributes = mutableListOf(getSwiftPrivateAttribute() ?: swiftNameAttribute(propertyName.swiftName))

    declarationAttributes.addIfNotNull(getObjCDeprecationStatus())

    return ObjCProperty(
        name = name,
        comment = annotations.translateToObjCComment(),
        origin = getObjCExportStubOrigin(),
        type = type ?: ObjCIdType, //[ObjCIdType] temp fix, should be translated properly, see KT-65709
        propertyAttributes = attributes,
        setterName = if (setterName.isNullOrBlank()) null else setterName,
        getterName = getterName,
        declarationAttributes = declarationAttributes
    )
}

private val String.asSetterSelector: String
    get() = "set" + replaceFirstChar(Char::uppercase) + ":"