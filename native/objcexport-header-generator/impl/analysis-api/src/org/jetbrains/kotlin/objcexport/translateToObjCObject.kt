/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithModality
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration

context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun KaClassSymbol.translateToObjCObject(): ObjCClass? {
    require(classKind == KaClassKind.OBJECT || classKind == KaClassKind.COMPANION_OBJECT)
    if (!isVisibleInObjC()) return null

    val enumKind = this.classKind == KaClassKind.ENUM_CLASS
    val final = if (this is KaSymbolWithModality) this.modality == Modality.FINAL else false
    val name = getObjCClassOrProtocolName()
    val attributes = (if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()) + name.toNameAttributes()
    val comment: ObjCComment? = annotations.translateToObjCComment()
    val origin = getObjCExportStubOrigin()
    val superProtocols: List<String> = superProtocols()
    val categoryName: String? = null
    val generics: List<ObjCGenericTypeDeclaration> = emptyList()
    val superClass = translateSuperClass()

    val objectMembers = mutableListOf<ObjCExportStub>()
    objectMembers += translateToObjCConstructors()
    objectMembers += getDefaultMembers()
    objectMembers += declaredMemberScope.callables
        .sortedWith(StableCallableOrder)
        .flatMap { it.translateToObjCExportStub() }

    return ObjCInterfaceImpl(
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

context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaClassSymbol.getDefaultMembers(): List<ObjCExportStub> {

    val result = mutableListOf<ObjCExportStub>()

    result.add(
        ObjCMethod(
            null,
            null,
            false,
            ObjCInstanceType,
            listOf(getObjectInstanceSelector(this)),
            emptyList(),
            listOf(swiftNameAttribute("init()"))
        )
    )

    result.add(
        ObjCProperty(
            name = ObjCPropertyNames.objectPropertyName,
            comment = null,
            type = toPropertyType(),
            propertyAttributes = listOf("class", "readonly"),
            getterName = getObjectPropertySelector(this),
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
context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaClassSymbol.toPropertyType() = ObjCClassType(
    className = getObjCClassOrProtocolName().objCName,
    typeArguments = emptyList(),
    extras = objCTypeExtras {
        requiresForwardDeclaration = true
        originClassId = classId
    }
)

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getObjectInstanceSelector]
 */
context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun getObjectInstanceSelector(objectSymbol: KaClassSymbol): String {
    return objectSymbol.getObjCClassOrProtocolName(bareName = true)
        .objCName
        .replaceFirstChar(Char::lowercaseChar)
        .mangleIfReservedObjCName()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getObjectPropertySelector]
 */
context(KaSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun getObjectPropertySelector(descriptor: KaClassSymbol): String {
    val collides = ObjCPropertyNames.objectPropertyName == getObjectInstanceSelector(descriptor)
    return ObjCPropertyNames.objectPropertyName + (if (collides) "_" else "")
}
