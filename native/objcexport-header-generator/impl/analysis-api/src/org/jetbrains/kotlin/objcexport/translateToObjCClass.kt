package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCClass(): ObjCClass? {
    require(classKind == KtClassKind.CLASS || classKind == KtClassKind.ENUM_CLASS)
    if (!isVisibleInObjC()) return null

    val enumKind = this.classKind == KtClassKind.ENUM_CLASS
    val final = if (this is KtSymbolWithModality) this.modality == Modality.FINAL else false

    val name = getObjCClassOrProtocolName()
    val attributes = (if (enumKind || final) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()) + name.toNameAttributes()

    val comment: ObjCComment? = annotationsList.translateToObjCComment()
    val origin: ObjCExportStubOrigin = getObjCExportStubOrigin()

    val superClass = translateSuperClass()
    val superProtocols: List<String> = superProtocols()

    val members = buildList<ObjCExportStub> {
        /* The order of members tries to replicate the K1 implementation explicitly */
        this += translateToObjCConstructors()

        if (needsCompanionProperty) {
            this += buildCompanionProperty()
        }

        /* Special case so far: Just for 'Enum' we actually want to add this clone method to match K1 */
        if (classIdIfNonLocal == StandardClassIds.Enum) {
            this += cloneMethod
        }

        this += getDeclaredMemberScope().getCallableSymbols().sortedWith(StableCallableOrder)
            .mapNotNull { it.translateToObjCExportStub() }

        if (classKind == KtClassKind.ENUM_CLASS) {
            this += translateEnumMembers()
        }
    }

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

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtNonErrorClassType.getSuperClassName(): ObjCExportClassOrProtocolName? {
    val classSymbol = expandedClassSymbol ?: return null
    return classSymbol.getObjCClassOrProtocolName()
}

private val cloneMethod = ObjCMethod(
    selectors = listOf("clone"),
    comment = ObjCComment(contentLines = listOf("@note This method has protected visibility in Kotlin source and is intended only for use by subclasses.")),
    origin = null,
    returnType = ObjCIdType,
    parameters = emptyList(),
    isInstanceMethod = true,
    attributes = listOf(swiftNameAttribute("clone()"))
)
