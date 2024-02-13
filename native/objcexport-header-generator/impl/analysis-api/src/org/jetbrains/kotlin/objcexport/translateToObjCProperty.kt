/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCIdType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getPropertyMethodBridge
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtPropertySymbol.translateToObjCProperty(): ObjCProperty? {
    if (!isVisibleInObjC()) return null
    return buildProperty()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildProperty]
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtPropertySymbol.buildProperty(): ObjCProperty {
    val propertyName = getObjCPropertyName()
    val name = propertyName.objCName
    val bridge = getPropertyMethodBridge()
    val type = getter?.mapReturnType(bridge.returnBridge)
    val attributes = mutableListOf<String>()
    val setterName: String?

    if (!bridge.isInstance) {
        attributes += "class"
    }

    val propertySetter = setter
    // Note: the condition below is similar to "toObjCMethods" logic in [ObjCExportedInterface.createCodeSpec].
    val shouldBeSetterExposed = true //TODO: mapper.shouldBeExposed

    if (propertySetter != null && shouldBeSetterExposed) {
        val setterSelector = propertySetter.getSelector(bridge)
        setterName = if (setterSelector != "set" + name.replaceFirstChar(Char::uppercaseChar) + ":") setterSelector else null
    } else {
        attributes += "readonly"
        setterName = null
    }


    val getterSelector = getter?.getSelector(bridge)
    val getterName: String? = if (getterSelector != name && getterSelector?.isNotBlank() == true) getterSelector else null

    val declarationAttributes = mutableListOf(getSwiftPrivateAttribute() ?: swiftNameAttribute(propertyName.swiftName))

    //TODO: implement and use [org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver]
    //declarationAttributes.addIfNotNull(mapper.getDeprecation(property)?.toDeprecationAttribute())

    return ObjCProperty(
        name = name,
        comment = null,
        origin = getObjCExportStubOrigin(),
        type = type ?: ObjCIdType, //[ObjCIdType] temp fix, should be translated properly, see KT-65709
        propertyAttributes = attributes,
        setterName = if (setterName.isNullOrBlank()) null else setterName,
        getterName = getterName,
        declarationAttributes = declarationAttributes
    )
}