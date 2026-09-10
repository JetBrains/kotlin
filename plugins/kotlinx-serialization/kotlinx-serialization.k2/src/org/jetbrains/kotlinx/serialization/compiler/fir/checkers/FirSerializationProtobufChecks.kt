/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.protoNumberAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.protoOneOfAnnotationClassId

internal fun CheckerContext.checkProtobufProperties(
    properties: List<FirSerializableProperty>,
    reporter: DiagnosticReporter,
) {
    /*
    IMPORTANT! In protobuf filed numbers starts with 1
    Therefore, for correct calculations, we assume that the fields in the class are also numbered from 1

    The following notation is used in the comments:
    proto number - field number used when encoding with protobuf
    origin number - sequence number of field in the class, starts with 1
    custom number - redefined by @ProtoNumber annotation value of proto number

    Target proto number is origin number or if @ProtoNumber is specified a custom number
     */

    // origin number -> custom number
    val originToCustom = mutableMapOf<Int, Int>()

    properties.forEachIndexed { index, property ->

        val annotation =
            property.propertySymbol.resolvedAnnotationsWithArguments.getAnnotationByClassId(protoNumberAnnotationClassId, session)
                ?: return@forEachIndexed

        // TODO should we throw error if there is no `number` argument or there is evaluation error
        val argument = annotation.findArgumentByName(Name.identifier("number")) ?: return@forEachIndexed
        val literal = argument as? FirLiteralExpression ?: return@forEachIndexed
        val customNumber = (literal.value as? Number)?.toInt() ?: return@forEachIndexed

        // use +1 to follow the rule that fields are numbered from 1
        originToCustom[index + 1] = customNumber
    }

    // there is no ProtoNumber annotation
    if (originToCustom.isEmpty()) return

    // origin number -> proto number
    // proto id = null if there is no override annotation
    val originToProto = mutableMapOf<Int, Int?>()
    for (number in 1..properties.size) {
        // use -1 to follow the rule that fields are numbered from 1
        if (properties[number - 1].propertySymbol.getAnnotationByClassId(protoOneOfAnnotationClassId, session) != null) {
            // if property marked with ProtoOneOf annotation we should skip check field number for it
            // because filed number will be specified in heirs
            continue
        }

        originToProto[number] = originToCustom[number]
    }

    // proto id -> [list of origin fields numbers that uses it, null there is no annotation on a field]
    val duplicates = mutableMapOf<Int, MutableList<Int?>>()
    originToProto.forEach { [originNumber, protoNumber] ->
        if (protoNumber != null) {
            duplicates.getOrPut(protoNumber) { mutableListOf() }.add(originNumber)
        } else {
            duplicates.getOrPut(originNumber) { mutableListOf() }.add(null)
        }
    }

    originToProto.forEach { [originNumber, protoNumber] ->
        // skip fields without ProtoNumber annotation
        if (protoNumber == null) return@forEach

        val duplicates = duplicates.getValue(protoNumber)
        if (duplicates.size < 2) return@forEach

        // use -1 to follow the rule that fields are numbered from 1
        val property = properties[originNumber - 1]
        val annotation =
            property.propertySymbol.resolvedAnnotationsWithArguments.getAnnotationByClassId(protoNumberAnnotationClassId, session)

        val duplicateFieldsNames = duplicates.asSequence()
            // if fieldNumber == null it's mean that there is no custom annotation and proto number is an origin field number
            .map { number -> number ?: protoNumber }
            .filter { number -> number != originNumber }
            // use -1 to follow the rule that fields are numbered from 1
            .map { number -> properties[number - 1].propertySymbol.name }
            .joinToString()

        reporter.reportOn(
            source = annotation?.source,
            factory = FirSerializationErrors.PROTOBUF_PROTO_NUM_DUPLICATED,
            a = property.propertySymbol.name.asString(),
            b = duplicateFieldsNames
        )

    }
}

/**
 * `kotlinx-serialization-protobuf` annotations that only make sense for particular property types. Applying them
 * elsewhere silently does nothing, which is easy to get wrong. See KT-81042.
 */
internal fun CheckerContext.checkProtobufAnnotationTargets(
    classSymbol: FirClassSymbol<*>,
    properties: List<FirSerializableProperty>,
    reporter: DiagnosticReporter,
) {
    val classLookupTag = classSymbol.toLookupTag()
    for (property in properties) {
        if (!classLookupTag.isRealOwnerOf(property.propertySymbol)) continue

        val propertySymbol = property.propertySymbol
        // Do not account for custom serializers
        if (propertySymbol.getSerializableWith(session) != null) continue

        val type = propertySymbol.resolvedReturnType.fullyExpandedType()
        if (isOpaqueForProtobuf(type)) continue

        fun report(annotationClassId: ClassId, requirement: String, isApplicable: Boolean) {
            if (isApplicable) return
            val annotation = propertySymbol.getAnnotationByClassId(annotationClassId, session) ?: return
            reporter.reportOn(
                annotation.source ?: propertySymbol.source,
                FirSerializationErrors.PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE,
                annotationClassId.shortClassName.asString(),
                requirement,
                type
            )
        }

        report(
            SerializationAnnotations.protoPackedAnnotationClassId,
            "collections and arrays",
            isApplicable = isCollectionOrArray(type)
        )
        // @ProtoType selects the integer encoding of the property's tag. Collections, arrays and maps of such types
        // count too: RepeatedEncoder and MapRepeatedEncoder derive element and entry tags from that same tag.
        report(
            SerializationAnnotations.protoTypeAnnotationClassId,
            "integer properties, and collections or maps of them",
            isApplicable = isIntegerEncoded(type) ||
                    // An undeterminable element type must not be reported, hence `!= false`.
                    (isCollectionOrArray(type) && collectionOrArrayElementType(type)?.let { mayBeIntegerEncoded(it) } != false) ||
                    isMapWithIntegerEncodedEntries(type)
        )
        // The value of a oneof group is one of several message types, so anything that maps onto a scalar
        // protobuf field cannot hold it.
        report(
            SerializationAnnotations.protoOneOfAnnotationClassId,
            "properties of non-scalar types",
            isApplicable = !type.isPrimitiveOrNullablePrimitive
                    && type.classId != StandardClassIds.String
                    && type.classSymbolOrUpperBound(session)?.isEnumClass != true
        )
    }
}

private val INTEGER_ENCODED_CLASS_IDS = setOf(
    StandardClassIds.Byte, StandardClassIds.Short, StandardClassIds.Int, StandardClassIds.Long,
    StandardClassIds.Char, StandardClassIds.Boolean,
    StandardClassIds.UByte, StandardClassIds.UShort, StandardClassIds.UInt, StandardClassIds.ULong,
)

private fun isIntegerEncoded(type: ConeKotlinType): Boolean = type.classId in INTEGER_ENCODED_CLASS_IDS

private fun CheckerContext.mayBeIntegerEncoded(type: ConeKotlinType): Boolean =
    isIntegerEncoded(type) || isOpaqueForProtobuf(type)

/**
 * The cases where the declared type says nothing about what is finally encoded.
 *
 * Serializers supplied by `@Contextual` or by the file-level `@UseSerializers` / `@UseContextualSerialization` are
 * deliberately not consulted: reporting on those types is accepted.
 */
private fun CheckerContext.isOpaqueForProtobuf(type: ConeKotlinType): Boolean {
    if (type.lowerBoundIfFlexible().isTypeParameter) return true
    // A serializer attached to the type's own class, rather than to the property.
    if (type.getSerializableWith(session) != null) return true
    // A value class forwards the property's tag, integer type included, to the value it wraps.
    return type.toRegularClassSymbol()?.isInlineOrValue == true
}

private fun CheckerContext.isMapWithIntegerEncodedEntries(type: ConeKotlinType): Boolean {
    if (!isMap(type)) return false
    val arguments = type.typeArguments.map { it.type }
    // A star projection or fewer arguments than expected means we cannot tell, so stay silent.
    return arguments.size < 2 || arguments.any { it == null || mayBeIntegerEncoded(it) }
}

private fun CheckerContext.isMap(type: ConeKotlinType): Boolean {
    val classSymbol = type.toRegularClassSymbol() ?: return false
    if (classSymbol.classId == StandardClassIds.Map) return true
    return classSymbol.getAllSubstitutedSupertypes(session).any { it.classId == StandardClassIds.Map }
}

private fun CheckerContext.isCollectionOrArray(type: ConeKotlinType): Boolean {
    if (type.isNonPrimitiveArray || type.isPrimitiveOrUnsignedArray) return true
    val classSymbol = type.toRegularClassSymbol() ?: return false
    if (classSymbol.classId == StandardClassIds.Collection) return true
    return classSymbol.getAllSubstitutedSupertypes(session).any { it.classId == StandardClassIds.Collection }
}

private fun CheckerContext.collectionOrArrayElementType(type: ConeKotlinType): ConeKotlinType? {
    StandardClassIds.elementTypeByPrimitiveArrayType[type.classId]?.let { return it.constructClassLikeType() }
    StandardClassIds.elementTypeByUnsignedArrayType[type.classId]?.let { return it.constructClassLikeType() }
    // The use-site arguments carry the element type for arrays and for the standard collections, whose single
    // type parameter is the element. Walking supertypes instead would only yield the declaration's own parameter.
    if (type.isNonPrimitiveArray || type.classId in COLLECTION_CLASS_IDS) {
        return type.typeArguments.firstOrNull()?.type?.takeUnless { it.isTypeParameter }
    }
    // For a custom collection the element type is fixed by the supertype it implements, e.g. `MyList : List<Int>`.
    // Note that a @Serializable custom collection gets a CLASS-kind serializer and is encoded as a nested message,
    // so neither annotation reaches its elements — accepted as a false negative rather than guessed at here.
    val classSymbol = type.toRegularClassSymbol() ?: return null
    return classSymbol.getAllSubstitutedSupertypes(session)
        .find { it.classId == StandardClassIds.Collection }
        ?.typeArguments?.firstOrNull()?.type
        ?.takeUnless { it.isTypeParameter }
}

private val COLLECTION_CLASS_IDS = setOf(
    StandardClassIds.Collection, StandardClassIds.List, StandardClassIds.Set, StandardClassIds.Iterable,
    StandardClassIds.MutableCollection, StandardClassIds.MutableList, StandardClassIds.MutableSet,
    StandardClassIds.MutableIterable,
)
