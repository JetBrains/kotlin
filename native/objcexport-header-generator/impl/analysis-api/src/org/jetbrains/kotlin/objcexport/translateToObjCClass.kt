package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.objcexport.analysisApiUtils.members

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCClass(): ObjCClass? {
    require(classKind == KtClassKind.CLASS)
    if (!isVisibleInObjC()) return null

    val superClass = getSuperClassSymbolNotAny()
    val kotlinAnyName = "Base".getObjCKotlinStdlibClassOrProtocolName()
    val superName = if (superClass == null) kotlinAnyName else throw RuntimeException("Super class translation isn't implemented yet")
    val enumKind = this.classKind == KtClassKind.ENUM_CLASS
    val final = if (this is KtSymbolWithModality) this.modality == Modality.FINAL else false
    val attributes = if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()

    val name = getObjCClassOrProtocolName()
    val comment: ObjCComment? = annotationsList.toComment()
    val origin: ObjCExportStubOrigin = getObjCStubOrigin()
    val superProtocols: List<String> = superProtocols()
    val members: List<ObjCExportStub> = members().flatMap { it.translateToObjCExportStubs() }
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

private const val OBJC_SUBCLASSING_RESTRICTED = "objc_subclassing_restricted"

private fun abbreviate(name: String): String {
    val normalizedName = name
        .replaceFirstChar(Char::uppercaseChar)
        .replace("-|\\.".toRegex(), "_")

    val uppers = normalizedName.filterIndexed { index, character -> index == 0 || character.isUpperCase() }
    if (uppers.length >= 3) return uppers

    return normalizedName
}

/**
 * Not implemented
 */
private fun KtAnnotationsList.toComment(): ObjCComment? {
    return null
}

private val mustBeDocumentedAnnotationsStopList = setOf(
    StandardNames.FqNames.deprecated,
    KonanFqNames.objCName,
    KonanFqNames.shouldRefineInSwift
)