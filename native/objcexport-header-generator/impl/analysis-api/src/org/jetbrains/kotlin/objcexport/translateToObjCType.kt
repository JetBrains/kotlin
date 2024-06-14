package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isError
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCObjectType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.objCErrorType
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapType]
 */
context(KaSession, KtObjCExportSession)
internal fun KaType.translateToObjCType(typeBridge: TypeBridge): ObjCType {
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
            ObjCValueType.VECTOR_FLOAT_128 -> ObjCPrimitiveType.vectorFloat128
            ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, isBinaryRepresentationNullable())
        }
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceType]
 */
context(KaSession, KtObjCExportSession)
internal fun KaType.translateToObjCReferenceType(): ObjCReferenceType {
    return mapToReferenceTypeIgnoringNullability().withNullabilityOf(this)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceTypeIgnoringNullability]
 */
context(KaSession, KtObjCExportSession)
internal fun KaType.mapToReferenceTypeIgnoringNullability(): ObjCNonNullReferenceType {
    val fullyExpandedType = fullyExpandedType
    val classId = (fullyExpandedType as? KaClassType)?.classId

    if (isError) {
        return objCErrorType
    }

    if (isAnyType) {
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
        return translateToObjCObjectType()
    }

    /* Check if inline type represents 'regular' inline class */
    val classSymbol: KaClassOrObjectSymbol? = if (classId != null) findClass(classId) else null
    run check@{
        if (classId == null) return@check
        if (classSymbol !is KaNamedClassOrObjectSymbol) return@check
        if (classSymbol.isInline) return ObjCIdType
    }

    /* 'Irregular' inline class: Not marked as inline, but special K/N type that still gets inlined  */
    fullyExpandedType.getInlineTargetTypeOrNull()?.let { inlineTargetType ->
        return inlineTargetType.mapToReferenceTypeIgnoringNullability()
    }

    if (fullyExpandedType is KaClassType) {
        return if (classSymbol?.classKind == KaClassKind.INTERFACE) {
            ObjCProtocolType(
                protocolName = fullyExpandedType.objCTypeName,
                extras = objCTypeExtras {
                    requiresForwardDeclaration = true
                    originClassId = classId
                }
            )
        } else {
            ObjCClassType(
                className = fullyExpandedType.objCTypeName,
                typeArguments = translateTypeArgumentsToObjC(),
                extras = objCTypeExtras {
                    requiresForwardDeclaration = true
                    originClassId = classId
                }
            )
        }
    }

    if (fullyExpandedType is KaTypeParameterType) {
        val definingSymbol = fullyExpandedType.symbol.containingSymbol

        if (definingSymbol is KaCallableSymbol) {
            return ObjCIdType
        }

        if (definingSymbol is KaClassOrObjectSymbol && definingSymbol.classKind == KaClassKind.INTERFACE) {
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

context(KaSession, KtObjCExportSession)
private val KaClassType.objCTypeName: String
    get() {
        return findClass(classId)?.getObjCClassOrProtocolName()?.objCName
            ?: classId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName
    }

context(KaSession, KtObjCExportSession)
internal fun KaType.translateTypeArgumentsToObjC(): List<ObjCNonNullReferenceType> {
    if (this !is KaClassType) return emptyList()

    /* See special casing below */
    val isKnownCollectionType = classId in collectionClassIds

    return typeArguments.map { typeArgument ->
        when (typeArgument) {
            is KaStarTypeProjection -> ObjCIdType
            is KaTypeArgumentWithVariance -> {
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