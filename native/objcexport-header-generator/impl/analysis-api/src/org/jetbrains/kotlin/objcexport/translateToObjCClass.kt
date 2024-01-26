package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCClass(): ObjCClass? {
    require(classKind == KtClassKind.CLASS)
    if (!isVisibleInObjC()) return null

    val superClass = getSuperClassSymbolNotAny()
    val kotlinAnyName = getDefaultSuperClassOrProtocolName()
    val superName = if (superClass == null) kotlinAnyName else getSuperClassName()
    val enumKind = this.classKind == KtClassKind.ENUM_CLASS
    val final = if (this is KtSymbolWithModality) this.modality == Modality.FINAL else false
    val attributes = if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()

    val name = getObjCClassOrProtocolName()
    val comment: ObjCComment? = annotationsList.translateToObjCComment()
    val origin: ObjCExportStubOrigin = getObjCExportStubOrigin()
    val superProtocols: List<String> = superProtocols()

    val members: List<ObjCExportStub> = getAllMembers()
        .sortedWith(StableSymbolOrder)
        .flatMap { it.translateToObjCExportStubs() }

    val categoryName: String? = null
    val generics: List<ObjCGenericTypeDeclaration> = emptyList()
    val superClassGenerics: List<ObjCNonNullReferenceType> = emptyList()

    return ObjCInterfaceImpl(
        name.objCName,
        comment,
        origin,
        attributes,
        superProtocols,
        members,
        categoryName,
        generics,
        superName.objCName,
        superClassGenerics
    )
}

private fun abbreviate(name: String): String {
    val normalizedName = name
        .replaceFirstChar(Char::uppercaseChar)
        .replace("-|\\.".toRegex(), "_")

    val uppers = normalizedName.filterIndexed { index, character -> index == 0 || character.isUpperCase() }
    if (uppers.length >= 3) return uppers
    return normalizedName
}

/**
 * See issue KT-65384
 * And K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.translateClass]
 */
private fun KtClassOrObjectSymbol.getSuperClassName(): ObjCExportClassOrProtocolName {
    return ObjCExportClassOrProtocolName("UnimplementedSwiftName", "UnimplementedObjCName")
}