/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapType]
 */
internal fun ObjCExportContext.translateToObjCType(type: KaType, typeBridge: TypeBridge): ObjCType {
    return when (typeBridge) {
        is ReferenceBridge -> translateToObjCReferenceType(type)
        is BlockPointerBridge -> translateToObjCFunctionType(type, typeBridge)
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
            ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, kaSession.isBinaryRepresentationNullable(type))
        }
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceType]
 */
internal fun ObjCExportContext.translateToObjCReferenceType(type: KaType): ObjCReferenceType {
    val referenceType = mapToReferenceTypeIgnoringNullability(type)
    return kaSession.withNullabilityOf(referenceType, type)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceTypeIgnoringNullability]
 */
internal fun ObjCExportContext.mapToReferenceTypeIgnoringNullability(type: KaType): ObjCNonNullReferenceType {
    with(kaSession) {
        val fullyExpandedType = type.fullyExpandedType
        val classId = (fullyExpandedType as? KaClassType)?.classId

        if (type.isError) {
            return objCErrorType
        }

        if (type.isAnyType) {
            return ObjCIdType
        }

        if (classId in hiddenClassIds) {
            return ObjCIdType
        }

        if (type.symbol != null && isVisibleInObjC(type.symbol) == false) {
            return ObjCIdType
        }

        /* Priority: Check if this type is mapped into a known ObjCType (e.g. String -> NSString */
        translateToMappedObjCTypeOrNull(type)?.let { mappedObjCType ->
            return mappedObjCType
        }

        /* Kotlin Native Primitive Type cannot be mapped to a reference type here */
        if (classId in kotlinNativePrimitiveClassIds) {
            return ObjCIdType
        }

        if (isObjCObjectType(type)) {
            // KT-65891: mapObjCObjectReferenceTypeIgnoringNullability
            return translateToObjCObjectType(type)
        }

        /* Check if inline type represents 'regular' inline class */
        val classSymbol: KaClassSymbol? = if (classId != null) findClass(classId) else null
        run check@{
            if (classId == null) return@check
            if (classSymbol !is KaNamedClassSymbol) return@check
            if (classSymbol.isInline) return ObjCIdType
        }

        /* 'Irregular' inline class: Not marked as inline, but special K/N type that still gets inlined  */
        getInlineTargetTypeOrNull(fullyExpandedType)?.let { inlineTargetType ->
            return mapToReferenceTypeIgnoringNullability(inlineTargetType)
        }

        if (fullyExpandedType is KaClassType) {
            return if (classSymbol?.classKind == KaClassKind.INTERFACE) {
                ObjCProtocolType(
                    protocolName = getObjCTypeName(fullyExpandedType),
                    extras = objCTypeExtras {
                        requiresForwardDeclaration = true
                        originClassId = classId
                    }
                )
            } else {
                ObjCClassType(
                    className = getObjCTypeName(fullyExpandedType),
                    typeArguments = translateTypeArgumentsToObjC(type),
                    extras = objCTypeExtras {
                        requiresForwardDeclaration = true
                        originClassId = classId
                    }
                )
            }
        }

        if (fullyExpandedType is KaTypeParameterType) {
            val definingSymbol = fullyExpandedType.symbol.containingDeclaration

            if (definingSymbol is KaCallableSymbol) {
                return ObjCIdType
            }

            if (definingSymbol is KaClassSymbol && definingSymbol.classKind == KaClassKind.INTERFACE) {
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

}

private fun ObjCExportContext.getObjCTypeName(type: KaClassType): String {
    val clazz = kaSession.findClass(type.classId)
    return if (clazz != null) {
        getObjCClassOrProtocolName(clazz).objCName
    } else {
        exportSession.getObjCKotlinStdlibClassOrProtocolName(type.classId.shortClassName.asString()).objCName
    }
}

internal fun ObjCExportContext.translateTypeArgumentsToObjC(type: KaType): List<ObjCNonNullReferenceType> {
    if (type !is KaClassType) return emptyList()

    /* See special casing below */
    val isKnownCollectionType = type.classId in collectionClassIds

    return type.typeArguments.map { typeArgument ->
        when (typeArgument) {
            is KaStarTypeProjection -> ObjCIdType
            is KaTypeArgumentWithVariance -> {
                val isMarkedNullable = with(kaSession) { typeArgument.type.isMarkedNullable }
                /*
                Kotlin `null` keys and values are represented as `NSNull` singleton in collections
                */
                if (isKnownCollectionType && isMarkedNullable) return@map ObjCIdType
                mapToReferenceTypeIgnoringNullability(typeArgument.type)
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