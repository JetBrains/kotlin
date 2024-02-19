package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isError
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCObjectType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.objCErrorType


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapType]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToObjCType(typeBridge: TypeBridge): ObjCType {
    return when (typeBridge) {
        is ReferenceBridge -> this.translateToObjCReferenceType()
        is BlockPointerBridge -> this.translateToObjCFunctionType(typeBridge)
        is ValueTypeBridge -> when (typeBridge.objCValueType) {
            ObjCValueType.BOOL -> ObjCPrimitiveType.BOOL
            ObjCValueType.UNICHAR -> ObjCPrimitiveType.unichar
            ObjCValueType.CHAR -> ObjCPrimitiveType.int8_t
            ObjCValueType.SHORT -> ObjCPrimitiveType.int16_t
            ObjCValueType.INT -> ObjCPrimitiveType.int32_t
            ObjCValueType.LONG_LONG -> ObjCPrimitiveType.int64_t
            ObjCValueType.UNSIGNED_CHAR -> ObjCPrimitiveType.uint8_t
            ObjCValueType.UNSIGNED_SHORT -> ObjCPrimitiveType.uint16_t
            ObjCValueType.UNSIGNED_INT -> ObjCPrimitiveType.uint32_t
            ObjCValueType.UNSIGNED_LONG_LONG -> ObjCPrimitiveType.uint64_t
            ObjCValueType.FLOAT -> ObjCPrimitiveType.float
            ObjCValueType.DOUBLE -> ObjCPrimitiveType.double
            ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, isBinaryRepresentationNullable())
        }
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceType]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToObjCReferenceType(): ObjCReferenceType {
    return mapToReferenceTypeIgnoringNullability().withNullabilityOf(this)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceTypeIgnoringNullability]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.mapToReferenceTypeIgnoringNullability(): ObjCNonNullReferenceType {
    val fullyExpandedType = fullyExpandedType
    val classId = (fullyExpandedType as? KtNonErrorClassType)?.classId

    if (isError) {
        return objCErrorType
    }

    if (isAny) {
        return ObjCIdType
    }

    if (classId in hiddenClassIds) {
        return ObjCIdType
    }

    /* Priority: Check if this type is mapped into a known ObjCType (e.g. String -> NSString */
    translateToMappedObjCTypeOrNull()?.let { mappedObjCType ->
        return mappedObjCType
    }

    /* Kotlin Native Primitive Type cannot be mapped to a reference type here */
    if (classId in kotlinNativePrimitiveClassIds) {
        return ObjCIdType
    }

    if (isObjCObjectType()) {
        // KT-65891: mapObjCObjectReferenceTypeIgnoringNullability
        return ObjCIdType
    }

    /* Check if inline type represents 'regular' inline class */
    val classSymbol: KtClassOrObjectSymbol? = if (classId != null) getClassOrObjectSymbolByClassId(classId) else null
    run check@{
        if (classId == null) return@check
        if (classSymbol !is KtNamedClassOrObjectSymbol) return@check
        if (classSymbol.isInline) return ObjCIdType
    }

    /* 'Irregular' inline class: Not marked as inline, but special K/N type that still gets inlined  */
    fullyExpandedType.getInlineTargetTypeOrNull()?.let { inlineTargetType ->
        return inlineTargetType.mapToReferenceTypeIgnoringNullability()
    }

    if (fullyExpandedType is KtNonErrorClassType) {

        // TODO NOW: create type translation test
        return if (classSymbol?.classKind == KtClassKind.INTERFACE) {
            ObjCProtocolType(fullyExpandedType.objCTypeName, classId)
        } else {
            ObjCClassType(fullyExpandedType.objCTypeName, translateTypeArgumentsToObjC(), classId)
        }
    }

    if (fullyExpandedType is KtTypeParameterType) {
        val definingSymbol = fullyExpandedType.symbol.getContainingSymbol()

        if (definingSymbol is KtCallableSymbol) {
            return ObjCIdType
        }

        if (definingSymbol is KtClassOrObjectSymbol && definingSymbol.classKind == KtClassKind.INTERFACE) {
            return ObjCIdType
        }
        /*
        Todo: K1 has some name mangling logic here?
        */
        return ObjCGenericTypeParameterUsage(fullyExpandedType.name.asString().toValidObjCSwiftIdentifier())
    }

    /* We cannot translate this, lets try to be lenient and emit the error type? */
    return objCErrorType
}

context(KtAnalysisSession, KtObjCExportSession)
private val KtNonErrorClassType.objCTypeName: String
    get() {
        return getClassOrObjectSymbolByClassId(classId)?.getObjCClassOrProtocolName()?.objCName
            ?: classId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName
    }

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateTypeArgumentsToObjC(): List<ObjCNonNullReferenceType> {
    if (this !is KtNonErrorClassType) return emptyList()

    /* See special casing below */
    val isKnownCollectionType = classId in collectionClassIds

    return ownTypeArguments.map { typeArgument ->
        when (typeArgument) {
            is KtStarTypeProjection -> ObjCIdType
            is KtTypeArgumentWithVariance -> {
                /*
                Kotlin `null` keys and values are represented as `NSNull` singleton in collections
                */
                if (isKnownCollectionType && typeArgument.type.isMarkedNullable) return@map ObjCIdType
                typeArgument.type.mapToReferenceTypeIgnoringNullability()
            }
        }
    }
}


/**
 * Types to be "hidden" during mapping, i.e., represented as `id`.
 *
 * Currently, it contains super types of classes handled by custom type mappers.
 * Note: It can be generated programmatically, but requires stdlib in this case.
 */
private val hiddenClassIds: Set<ClassId> = listOf(
    "kotlin.Any",
    "kotlin.CharSequence",
    "kotlin.Comparable",
    "kotlin.Function",
    "kotlin.Number",
    "kotlin.collections.Collection",
    "kotlin.collections.Iterable",
    "kotlin.collections.MutableCollection",
    "kotlin.collections.MutableIterable"
).map { ClassId.topLevel(FqName(it)) }.toSet()


private val kotlinNativePrimitiveClassIds: Set<ClassId> =
    KonanPrimitiveType.entries.map { it.classId }.toSet()

private val collectionClassIds = setOf(
    StandardClassIds.List, StandardClassIds.MutableList,
    StandardClassIds.Set, StandardClassIds.MutableSet,
    StandardClassIds.Map, StandardClassIds.MutableMap
)