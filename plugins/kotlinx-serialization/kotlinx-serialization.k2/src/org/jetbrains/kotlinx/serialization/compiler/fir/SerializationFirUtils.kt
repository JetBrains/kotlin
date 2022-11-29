/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.inheritableSerialInfoClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.metaSerializableAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialInfoClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialNameAnnotationClassId

object AnnotationParameterNames {
    val VALUE = Name.identifier("value")
    val WITH = Name.identifier("with")
    val FOR_CLASS = Name.identifier("forClass")
}

// ---------------------- annotations utils ----------------------

val FirBasedSymbol<*>.isSerialInfoAnnotation: Boolean
    get() = hasAnnotation(serialInfoClassId)
            || hasAnnotation(inheritableSerialInfoClassId)
            || hasAnnotation(metaSerializableAnnotationClassId)

val FirBasedSymbol<*>.isInheritableSerialInfoAnnotation: Boolean
    get() = hasAnnotation(inheritableSerialInfoClassId)

val FirBasedSymbol<*>.serialNameAnnotation: FirAnnotation?
    get() = resolvedAnnotationsWithArguments.getAnnotationByClassId(serialNameAnnotationClassId)

val FirBasedSymbol<*>.serialNameValue: String?
    get() = serialNameAnnotation?.getStringArgument(AnnotationParameterNames.VALUE)

val FirBasedSymbol<*>.serialRequired: Boolean
    get() = hasAnnotation(SerializationAnnotations.requiredAnnotationClassId)

val FirBasedSymbol<*>.hasSerialTransient: Boolean
    get() = serialTransientAnnotation != null

val FirBasedSymbol<*>.serialTransientAnnotation: FirAnnotation?
    get() = getAnnotationByClassId(SerializationAnnotations.serialTransientClassId)

val FirClassSymbol<*>.hasSerializableAnnotation: Boolean
    get() = serializableAnnotation(needArguments = false) != null

fun FirBasedSymbol<*>.serializableAnnotation(needArguments: Boolean): FirAnnotation? {
    val annotations = if (needArguments) resolvedAnnotationsWithClassIds else resolvedAnnotationsWithArguments
    return annotations.serializableAnnotation()
}

fun List<FirAnnotation>.serializableAnnotation(): FirAnnotation? {
    return getAnnotationByClassId(SerializationAnnotations.serializableAnnotationClassId)
}

val FirClassSymbol<*>.hasSerializableAnnotationWithoutArgs: Boolean
    get() = serializableAnnotation(needArguments = false)?.let {
        if (it is FirAnnotationCall) {
            it.arguments.isEmpty()
        } else {
            it.argumentMapping.mapping.isEmpty()
        }
    } ?: false

internal val FirBasedSymbol<*>.serializableWith: ConeKotlinType?
    get() = serializableAnnotation(needArguments = true)?.getKClassArgument(AnnotationParameterNames.WITH)

internal val List<FirAnnotation>.serializableWith: ConeKotlinType?
    get() = serializableAnnotation()?.getKClassArgument(AnnotationParameterNames.WITH)

fun FirAnnotation.getGetKClassArgument(name: Name): FirGetClassCall? {
    return findArgumentByName(name) as? FirGetClassCall
}

internal val FirClassSymbol<*>.serializerAnnotation: FirAnnotation?
    get() = getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId)

// ---------------------- class utils ----------------------
internal val FirClassSymbol<*>.serializerForClass: ConeKotlinType?
    get() = resolvedAnnotationsWithArguments
        .getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId)
        ?.getKClassArgument(AnnotationParameterNames.FOR_CLASS)

internal val FirClassLikeDeclaration.serializerFor: FirGetClassCall?
    get() = getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId)
        ?.getGetKClassArgument(AnnotationParameterNames.FOR_CLASS)

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isInternallySerializableObject: Boolean
    get() = classKind.isObject && hasSerializableOrMetaAnnotationWithoutArgs

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isSerializableObject: Boolean
    get() = classKind.isObject && hasSerializableOrMetaAnnotation

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isSealedSerializableInterface: Boolean
    get() = classKind.isInterface && rawStatus.modality == Modality.SEALED && hasSerializableOrMetaAnnotation

context(FirSession)
val FirClassSymbol<*>.hasSerializableOrMetaAnnotation: Boolean
    get() = hasSerializableAnnotation || hasMetaSerializableAnnotation

context(FirSession)
val FirClassSymbol<*>.hasMetaSerializableAnnotation: Boolean
    get() = predicateBasedProvider.matches(FirSerializationPredicates.hasMetaAnnotation, this)

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = isSerializableObject
            || isSerializableEnum
            || (classKind == ClassKind.CLASS && hasSerializableOrMetaAnnotation)
            || isSealedSerializableInterface

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isInternalSerializable: Boolean
    get() {
        if (!classKind.isClass) return false
        return hasSerializableOrMetaAnnotationWithoutArgs
    }

context(FirSession)
val FirClassSymbol<*>.hasSerializableOrMetaAnnotationWithoutArgs: Boolean
    get() = hasSerializableAnnotationWithoutArgs || (!hasSerializableAnnotation && hasMetaSerializableAnnotation)

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isAbstractOrSealedSerializableClass: Boolean
    get() = isInternalSerializable && (rawStatus.modality == Modality.ABSTRACT || rawStatus.modality == Modality.SEALED)

/**
 * Check that class is enum and marked by `Serializable` or meta-serializable annotation.
 */
context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isSerializableEnum: Boolean
    get() = classKind.isEnumClass && hasSerializableOrMetaAnnotation

internal fun FirClassSymbol<*>.isFinalOrOpen(): Boolean {
    val modality = rawStatus.modality
    // null means default modality, final
    return (modality == null || modality == Modality.FINAL || modality == Modality.OPEN)
}

context(FirSession)
val FirClassSymbol<*>.isEnumWithLegacyGeneratedSerializer: Boolean
    get() = classKind.isEnumClass && dependencySerializationInfoProvider.useGeneratedEnumSerializer && hasSerializableOrMetaAnnotationWithoutArgs

context(FirSession)
val FirClassSymbol<*>.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && isFinalOrOpen()) || isEnumWithLegacyGeneratedSerializer

// ---------------------- type utils ----------------------

val ConeKotlinType.isKSerializer: Boolean
    get() = classId == SerialEntityNames.KSERIALIZER_CLASS_ID

fun ConeKotlinType.serializerForType(session: FirSession): ConeKotlinType? {
    return this.fullyExpandedType(session)
        .toRegularClassSymbol(session)
        ?.resolvedSuperTypes
        ?.find { it.isKSerializer }
        ?.typeArguments
        ?.firstOrNull()
        ?.type
}

val ConeKotlinType.isTypeParameter: Boolean
    get() = this is ConeTypeParameterType

context(FirSession)
val ConeKotlinType.isGeneratedSerializableObject: Boolean
    get() = toRegularClassSymbol(this@FirSession)?.let { it.classKind.isObject && it.hasSerializableOrMetaAnnotationWithoutArgs } ?: false
