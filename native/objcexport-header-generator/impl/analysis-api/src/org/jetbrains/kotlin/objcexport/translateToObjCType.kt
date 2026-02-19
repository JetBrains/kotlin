/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.*
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
        is BlockPointerBridge -> translateToObjCFunctionType(type, typeBridge.returnsVoid)
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
            ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, analysisSession.isBinaryRepresentationNullable(type))
        }
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceType]
 */
internal fun ObjCExportContext.translateToObjCReferenceType(type: KaType): ObjCReferenceType {
    val referenceType = mapToReferenceTypeIgnoringNullability(type)
    return analysisSession.withNullabilityOf(referenceType, type)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceTypeIgnoringNullability]
 */
internal fun ObjCExportContext.mapToReferenceTypeIgnoringNullability(type: KaType): ObjCNonNullReferenceType {
    with(analysisSession) {

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

        /**
         * Most [KaTypeParameterType] should be translated as generics, but there are set of 3 exceptions (see below)
         */
        if (fullyExpandedType is KaTypeParameterType) {

            val definingSymbol = fullyExpandedType.symbol.containingDeclaration
            val isLocal = type.isFromLocalSymbol

            if (definingSymbol is KaClassSymbol && definingSymbol.classKind == KaClassKind.INTERFACE) {
                /**
                 * 1. When type parameter defined in interface
                 *
                 * interface Foo<Bar> {
                 *   val bar: Bar
                 * }
                 */
                return ObjCIdType
            } else if (definingSymbol is KaCallableSymbol) {
                /**
                 * 2. When type parameter defined in callable it should be translated either as `id` or upper bound
                 *
                 * class Foo {
                 *   fun <T> bar(t: T)
                 * }
                 */
                val callableUpperBound = definingSymbol.getUpperBoundOfCallableSymbol(fullyExpandedType)
                return if (callableUpperBound == null) {
                    ObjCIdType
                } else {
                    mapToReferenceTypeIgnoringNullability(callableUpperBound)
                }
            } else {
                /**
                 * 3. Objective-C doesn't support upper bounds, so we try to find most proper one
                 * 3.1 See detailed case doc at [ObjCExportContext.classifierContext]
                 * 3.2 When type parameter symbol is local
                 */
                val classifierContextIsNotContainingDeclaration = if (definingSymbol != null && classifierContext != null) {
                    /**
                     * Originally symbols were compared directly, but that triggered SOE
                     * So it's changed to classId check
                     * See details at KT-71780
                     */
                    (definingSymbol as? KaClassSymbol)?.classId != classifierContext.classId
                } else false

                val upperBound = if (classifierContextIsNotContainingDeclaration) {
                    findUpperBoundMatchingTypeParameter(definingSymbol as KaClassSymbol, fullyExpandedType) ?: return ObjCIdType
                } else if (isLocal) {
                    findUpperBoundMatchingTypeParameter(definingSymbol as KaClassSymbol, fullyExpandedType)
                } else null

                return if (upperBound != null) {
                    mapToReferenceTypeIgnoringNullability(upperBound)
                } else {
                    ObjCGenericTypeParameterUsage(fullyExpandedType.name.asString().toValidObjCSwiftIdentifier())
                }
            }
        }

        /* We cannot translate this, lets try to be lenient and emit the error type? */
        return objCErrorType
    }

}

private fun ObjCExportContext.getObjCTypeName(type: KaClassType): String {
    val clazz = analysisSession.findClass(type.classId)
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
                val isNullable = with(analysisSession) { typeArgument.type.isNullable }
                /*
                Kotlin `null` keys and values are represented as `NSNull` singleton in collections
                */
                if (isKnownCollectionType && isNullable) return@map ObjCIdType
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

/**
 * 1. We try to find upper bound from type parameters
 * ```kotlin
 * fun <A> foo(a: A)
 * ```
 * 2. If upper bound is another [KaTypeParameterType] we traverse through all symbol references and find upper bound
 * ```kotlin
 * class Foo<A> {
 *   fun <B : A> foo(a: B)
 * }
 * ```
 */
@OptIn(KaExperimentalApi::class)
private fun KaCallableSymbol.getUpperBoundOfCallableSymbol(type: KaTypeParameterType): KaType? {

    fun KaTypeParameterType.traverseParentUpperBounds(): KaType? {
        val bound = symbol.upperBound ?: return null
        return if (bound is KaTypeParameterType) {
            bound.traverseParentUpperBounds()
        } else bound
    }

    val bound = typeParameters.findAssociatedTypeParameterUpperBound(type)
    return if (bound is KaTypeParameterType) bound.traverseParentUpperBounds() else bound
}

private val KaType.isFromLocalSymbol: Boolean
    get() {
        return if (this is KaTypeParameterType) this.symbol.isLocal else this.symbol?.isLocal ?: false
    }

@OptIn(KaExperimentalApi::class)
private fun findUpperBoundMatchingTypeParameter(
    classSymbol: KaClassSymbol?,
    type: KaTypeParameterType,
): KaType? {
    return classSymbol?.typeParameters.findAssociatedTypeParameterUpperBound(type)
}

private fun List<KaTypeParameterSymbol>?.findAssociatedTypeParameterUpperBound(type: KaTypeParameterType): KaType? {
    if (this == null) return null
    forEach { parameterSymbol ->
        if (parameterSymbol == type.symbol) return parameterSymbol.upperBound
    }
    return null
}

/**
 * ObjC doesn't support multiple upper bounds, so we take the first one
 */
private val KaTypeParameterSymbol.upperBound: KaType?
    get() {
        return upperBounds.firstOrNull()
    }