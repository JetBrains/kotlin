package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCClass(): ObjCClass? {
    require(classKind == KtClassKind.CLASS)
    if (!isVisibleInObjC()) return null

    val enumKind = this.classKind == KtClassKind.ENUM_CLASS
    val final = if (this is KtSymbolWithModality) this.modality == Modality.FINAL else false

    val name = getObjCClassOrProtocolName()
    val attributes = (if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()) + name.toNameAttributes()

    val comment: ObjCComment? = annotationsList.translateToObjCComment()
    val origin: ObjCExportStubOrigin = getObjCExportStubOrigin()

    val superClass = translateSuperClass()
    val superProtocols: List<String> = superProtocols()

    val members: List<ObjCExportStub> = getMemberScope().getCallableSymbols().plus(getMemberScope().getConstructors())
        .sortedWith(StableCallableOrder)
        .flatMap { it.translateToObjCExportStubs() }
        .toList()

    val categoryName: String? = null

    val generics: List<ObjCGenericTypeDeclaration> = typeParameters.map { typeParameter ->
        ObjCGenericTypeParameterDeclaration(
            typeParameter.nameOrAnonymous.asString().toValidObjCSwiftIdentifier(),
            ObjCVariance.fromKotlinVariance(typeParameter.variance)
        )
    }

    return ObjCInterfaceImpl(
        name = name.objCName,
        comment = comment,
        origin = origin,
        attributes = attributes,
        superProtocols = superProtocols,
        members = members,
        categoryName = categoryName,
        generics = generics,
        superClass = superClass.superClassName.objCName,
        superClassGenerics = superClass.superClassGenerics
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

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtNonErrorClassType.getSuperClassName(): ObjCExportClassOrProtocolName? {
    val classSymbol = expandedClassSymbol ?: return null
    return classSymbol.getObjCClassOrProtocolName()
}