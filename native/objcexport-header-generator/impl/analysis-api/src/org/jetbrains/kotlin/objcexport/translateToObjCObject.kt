package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCompanion
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCObject(): ObjCClass? {
    require(classKind == KtClassKind.OBJECT || classKind == KtClassKind.COMPANION_OBJECT)
    if (!isVisibleInObjC()) return null

    val enumKind = this.classKind == KtClassKind.ENUM_CLASS
    val final = if (this is KtSymbolWithModality) this.modality == Modality.FINAL else false
    val name = getObjCClassOrProtocolName()
    val attributes = (if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()) + name.toNameAttributes()
    val comment: ObjCComment? = annotationsList.translateToObjCComment()
    val origin: ObjCExportStubOrigin = getObjCExportStubOrigin()
    val superProtocols: List<String> = superProtocols()
    val categoryName: String? = null
    val generics: List<ObjCGenericTypeDeclaration> = emptyList()
    val superClass = translateSuperClass()

    val objectMembers = mutableListOf<ObjCExportStub>()
    objectMembers += translateToObjCConstructors()
    objectMembers += getDefaultMembers()
    objectMembers += getDeclaredMemberScope().getCallableSymbols()
        .sortedWith(StableCallableOrder)
        .mapNotNull { it.translateToObjCExportStub() }

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

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.getDefaultMembers(): List<ObjCExportStub> {

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
context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.toPropertyType() = ObjCClassType(
    getObjCClassOrProtocolName().objCName,
    emptyList(),
    classIdIfNonLocal!!
)

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getObjectInstanceSelector]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun getObjectInstanceSelector(objectSymbol: KtClassOrObjectSymbol): String {
    return if (objectSymbol.isCompanion) ObjCPropertyNames.companionObjectPropertyName
    else objectSymbol.getObjCClassOrProtocolName().objCName.lowercase()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getObjectPropertySelector]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun getObjectPropertySelector(descriptor: KtClassOrObjectSymbol): String {
    val collides = ObjCPropertyNames.objectPropertyName == getObjectInstanceSelector(descriptor)
    return ObjCPropertyNames.objectPropertyName + (if (collides) "_" else "")
}
