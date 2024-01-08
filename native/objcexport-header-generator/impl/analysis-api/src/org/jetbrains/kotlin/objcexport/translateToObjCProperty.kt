/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
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
    val getterBridge = getter?.bridgeMethodImpl()
    val type = getter?.mapReturnType(getterBridge!!.returnBridge)
    val attributes = mutableListOf<String>()
    val setterName: String?

    if (!getterBridge!!.isInstance) {
        attributes += "class"
    }

    val propertySetter = setter
    // Note: the condition below is similar to "toObjCMethods" logic in [ObjCExportedInterface.createCodeSpec].
    val shouldBeSetterExposed = true //TODO: mapper.shouldBeExposed

    if (propertySetter != null && shouldBeSetterExposed) {
        val setterSelector = propertySetter.getSelector()
        setterName = if (setterSelector != "set" + name.replaceFirstChar(Char::uppercaseChar) + ":") setterSelector else null
    } else {
        attributes += "readonly"
        setterName = null
    }

    val getterName = null //TODO: Fix and use getter.getSelector(), it should return name when it's available

    val declarationAttributes = mutableListOf(getSwiftPrivateAttribute() ?: swiftNameAttribute(propertyName.swiftName))

    //TODO: implement and use [org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver]
    //declarationAttributes.addIfNotNull(mapper.getDeprecation(property)?.toDeprecationAttribute())

    return ObjCProperty(
        name,
        null,
        null,
        type!!,
        attributes,
        setterName,
        getterName,
        declarationAttributes
    )
}