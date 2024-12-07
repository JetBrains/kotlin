/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration

fun ObjCExportContext.translateToObjCObject(symbol: KaClassSymbol): ObjCClass? = withClassifierContext(symbol) {
    require(symbol.classKind == KaClassKind.OBJECT || symbol.classKind == KaClassKind.COMPANION_OBJECT)
    if (!analysisSession.isVisibleInObjC(symbol)) return@withClassifierContext null

    val enumKind = symbol.classKind == KaClassKind.ENUM_CLASS
    val final = symbol.modality == KaSymbolModality.FINAL
    val name = getObjCClassOrProtocolName(symbol)
    val attributes = (if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()) + name.toNameAttributes()
    val comment: ObjCComment? = analysisSession.translateToObjCComment(symbol.annotations)
    val origin = analysisSession.getObjCExportStubOrigin(symbol)
    val superProtocols: List<String> = superProtocols(symbol)
    val categoryName: String? = null
    val generics: List<ObjCGenericTypeDeclaration> = emptyList()
    val superClass = translateSuperClass(symbol)

    val objectMembers = mutableListOf<ObjCExportStub>()
    objectMembers += translateToObjCConstructors(symbol)
    objectMembers += getDefaultMembers(symbol, objectMembers)
    objectMembers += with(analysisSession) {
        symbol.declaredMemberScope.callables
            .sortedWith(getStableCallableOrder())
            .flatMap { translateToObjCExportStub(it) }
    }

    ObjCInterfaceImpl(
        name = name.objCName,
        comment = comment,
        origin = origin,
        attributes = attributes,
        superProtocols = superProtocols,
        members = objectMembers,
        categoryName = categoryName,
        generics = generics,
        superClass = superClass.superClassName.objCName,
        superClassGenerics = superClass.superClassGenerics
    )
}

private fun ObjCExportContext.getDefaultMembers(symbol: KaClassSymbol, members: List<ObjCExportStub>): List<ObjCExportStub> {

    val result = mutableListOf<ObjCExportStub>()

    result.addIfNotNull(addInitIfNeeded(symbol, members))

    result.add(
        ObjCProperty(
            name = ObjCPropertyNames.objectPropertyName,
            comment = null,
            type = toPropertyType(symbol),
            propertyAttributes = listOf("class", "readonly"),
            getterName = getObjectPropertySelector(symbol),
            declarationAttributes = listOf(swiftNameAttribute(ObjCPropertyNames.objectPropertyName)),
            origin = null
        )
    )
    return result
}

/**
 * TODO: Temp implementation
 * Use translateToObjCReferenceType() to make type
 * See also: [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceType]
 */
private fun ObjCExportContext.toPropertyType(symbol: KaClassSymbol) = ObjCClassType(
    className = getObjCClassOrProtocolName(symbol).objCName,
    typeArguments = emptyList(),
    extras = objCTypeExtras {
        requiresForwardDeclaration = true
        originClassId = symbol.classId
    }
)

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getObjectInstanceSelector]
 */
internal fun ObjCExportContext.getObjectInstanceSelector(objectSymbol: KaClassSymbol): String {
    return getObjCClassOrProtocolName(objectSymbol, bareName = true)
        .objCName
        .replaceFirstChar(Char::lowercaseChar)
        .mangleIfReservedObjCName()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getObjectPropertySelector]
 */
private fun ObjCExportContext.getObjectPropertySelector(descriptor: KaClassSymbol): String {
    val collides = ObjCPropertyNames.objectPropertyName == getObjectInstanceSelector(descriptor)
    return ObjCPropertyNames.objectPropertyName + (if (collides) "_" else "")
}
