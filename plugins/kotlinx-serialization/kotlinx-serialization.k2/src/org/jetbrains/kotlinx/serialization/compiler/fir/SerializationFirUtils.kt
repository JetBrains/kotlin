/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.inheritableSerialInfoClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.metaSerializableAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialInfoClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialNameAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SpecialBuiltins

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

context(CheckerContext)
val FirBasedSymbol<*>.hasAnySerialAnnotation: Boolean
    get() = serialNameValue != null || resolvedAnnotationsWithClassIds.any { it.annotationClassSymbol?.isSerialInfoAnnotation == true }

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

context(CheckerContext)
val FirClassSymbol<*>.hasSerializableOrMetaAnnotationWithoutArgs: Boolean
    get() = hasSerializableAnnotationWithoutArgs || (!hasSerializableAnnotation && hasMetaSerializableAnnotation)


context(CheckerContext)
val FirClassSymbol<*>.hasSerializableOrMetaAnnotation
    get() = hasSerializableAnnotation || hasMetaSerializableAnnotation

context(CheckerContext)
val FirClassSymbol<*>.hasMetaSerializableAnnotation: Boolean
    get() = metaSerializableAnnotation(needArguments = false) != null

context(CheckerContext)
fun FirClassSymbol<*>.metaSerializableAnnotation(needArguments: Boolean): FirAnnotation? {
    val annotations = if (needArguments) resolvedAnnotationsWithClassIds else resolvedAnnotationsWithArguments
    return annotations.firstOrNull { it.isMetaSerializableAnnotation }
}

context(CheckerContext)
val FirAnnotation.isMetaSerializableAnnotation: Boolean
    get() = annotationClassSymbol?.hasAnnotation(metaSerializableAnnotationClassId) ?: false

context(CheckerContext)
val ConeKotlinType.serializableWith: ConeKotlinType?
    get() = customAnnotations.serializableWith ?: toRegularClassSymbol(session)?.serializableWith

internal val FirBasedSymbol<*>.serializableWith: ConeKotlinType?
    get() = serializableAnnotation(needArguments = true)?.getKClassArgument(AnnotationParameterNames.WITH)

internal val List<FirAnnotation>.serializableWith: ConeKotlinType?
    get() = serializableAnnotation()?.getKClassArgument(AnnotationParameterNames.WITH)

internal val FirClassSymbol<*>.serializerForClass: ConeKotlinType?
    get() = resolvedAnnotationsWithArguments
        .getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId)
        ?.getKClassArgument(AnnotationParameterNames.FOR_CLASS)

internal val FirClassSymbol<*>.serializerAnnotation: FirAnnotation?
    get() = getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId)

context(CheckerContext)
private val FirAnnotation.annotationClassSymbol: FirRegularClassSymbol?
    get() = annotationTypeRef.coneType
        .fullyExpandedType(session)
        .toRegularClassSymbol(session)

// ---------------------- class utils ----------------------

internal val FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = this.isSerializableObject || this.isSerializableEnum || this.classKind == ClassKind.CLASS && hasSerializableAnnotation || this.isSealedSerializableInterface

internal val FirClassSymbol<*>.isSerializableObject: Boolean
    get() = classKind.isObject && hasSerializableAnnotation

internal val FirClassSymbol<*>.isInternallySerializableObject: Boolean
    get() = classKind.isObject && hasSerializableAnnotationWithoutArgs

internal val FirClassSymbol<*>.isSealedSerializableInterface: Boolean
    get() = classKind.isInterface && rawStatus.modality == Modality.SEALED && hasSerializableAnnotation

internal val FirClassSymbol<*>.isInternalSerializable: Boolean
    get() {
        if (!classKind.isClass) return false
        return hasSerializableAnnotationWithoutArgs
    }

internal val FirClassSymbol<*>.isAbstractOrSealedSerializableClass: Boolean
    get() = isInternalSerializable && (rawStatus.modality == Modality.ABSTRACT || rawStatus.modality == Modality.SEALED)

internal val FirClassSymbol<*>.isInternallySerializableEnum: Boolean
    get() = classKind.isEnumClass && hasSerializableAnnotationWithoutArgs

internal val FirClassSymbol<*>.isSerializableEnum: Boolean
    get() = classKind.isEnumClass && hasSerializableAnnotation

internal fun FirClassSymbol<*>.isFinalOrOpen(): Boolean {
    val modality = rawStatus.modality
    // null means default modality, final
    return (modality == null || modality == Modality.FINAL || modality == Modality.OPEN)
}

internal fun FirClassSymbol<*>.getSerializableClassSymbolIfCompanion(session: FirSession): FirClassSymbol<*>? {
    if (isSerializableObject) return this
    if (!isCompanion) return null
    val classDescriptor = (getContainingDeclarationSymbol(session) as? FirClassSymbol<*>) ?: return null
    if (!classDescriptor.shouldHaveGeneratedMethodsInCompanion) return null
    return classDescriptor
}

val FirClassSymbol<*>.hasCompanionObjectAsSerializer: Boolean
    get() = isInternallySerializableObject ||
            (this as? FirRegularClassSymbol)?.companionObjectSymbol?.serializerForClass == this.defaultType()

fun FirClassSymbol<*>.getSuperClassNotAny(session: FirSession): FirRegularClassSymbol? {
    return getSuperClassOrAny(session).takeUnless { it.classId == StandardClassIds.Any }
}

fun FirClassSymbol<*>.getSuperClassOrAny(session: FirSession): FirRegularClassSymbol {
    return resolvedSuperTypes.firstNotNullOfOrNull { superType ->
        superType.fullyExpandedType(session)
            .toRegularClassSymbol(session)
            ?.takeIf { it.classKind == ClassKind.CLASS }
    } ?: session.builtinTypes.anyType.toRegularClassSymbol(session) ?: error("Symbol for kotlin/Any not found")
}

context(CheckerContext)
val FirClassSymbol<*>?.classSerializer: FirClassSymbol<*>?
    get() {
        if (this == null) return null
        // serializer annotation on class?
        serializableWith?.let { return it.toRegularClassSymbol(session) }
        // companion object serializer?
        if (this is FirRegularClassSymbol && hasCompanionObjectAsSerializer) return companionObjectSymbol
        // can infer @Poly?
        polymorphicSerializerIfApplicableAutomatically?.let { return it }
        // default serializable?
        if (shouldHaveGeneratedSerializer) {
            // $serializer nested class
            return unsubstitutedScope(this@CheckerContext)
                .getSingleClassifier(SerialEntityNames.SERIALIZER_CLASS_NAME) as? FirClassSymbol<*>
        }
        return null
    }

context(CheckerContext)
val FirClassSymbol<*>.polymorphicSerializerIfApplicableAutomatically: FirClassSymbol<*>?
    get() {
        val serializerName = when {
            isInterface -> when (modality) {
                Modality.SEALED -> SpecialBuiltins.sealedSerializer
                else -> SpecialBuiltins.polymorphicSerializer
            }
            isInternalSerializable -> when (modality) {
                Modality.SEALED -> SpecialBuiltins.sealedSerializer
                Modality.ABSTRACT -> SpecialBuiltins.polymorphicSerializer
                else -> null
            }
            else -> null
        }
        return serializerName?.let { session.dependencySerializationInfoProvider.getClassFromSerializationPackage(Name.identifier(it)) }
    }

context(CheckerContext)
val FirClassSymbol<*>.isEnumWithLegacyGeneratedSerializer: Boolean
    get() = isInternallySerializableEnum && session.dependencySerializationInfoProvider.useGeneratedEnumSerializer

context(CheckerContext)
val FirClassSymbol<*>.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && (modality == Modality.FINAL || modality == Modality.OPEN)) || isEnumWithLegacyGeneratedSerializer

// ---------------------- type utils ----------------------

val ConeKotlinType.isKSerializer: Boolean
    get() = classId == SerialEntityNames.KSERIALIZER_CLASS_ID

fun ConeKotlinType.serializerForType(session: FirSession): ConeKotlinType? {
    return this.fullyExpandedType(session)
        .toRegularClassSymbol(session)
        ?.getAllSubstitutedSupertypes(session)
        ?.find { it.isKSerializer }
        ?.typeArguments
        ?.firstOrNull()
        ?.type
}

fun FirRegularClassSymbol.getAllSubstitutedSupertypes(session: FirSession): Set<ConeKotlinType> {
    val result = mutableSetOf<ConeKotlinType>()

    fun process(symbol: FirRegularClassSymbol, substitutor: ConeSubstitutor) {
        for (superType in symbol.resolvedSuperTypes) {
            if (result.add(substitutor.substituteOrSelf(superType))) {
                val superClassSymbol = superType.fullyExpandedType(session).toRegularClassSymbol(session) ?: continue
                val superSubstitutor =
                    (superType as? ConeLookupTagBasedType)?.let { createSubstitutionForSupertype(it, session) } ?: ConeSubstitutor.Empty
                process(superClassSymbol, ChainedSubstitutor(superSubstitutor, substitutor))
            }
        }
    }

    process(this, ConeSubstitutor.Empty)
    return result
}

val ConeKotlinType.isTypeParameter: Boolean
    get() = this is ConeTypeParameterType

context(CheckerContext)
val ConeKotlinType.overriddenSerializer: ConeKotlinType?
    get() = toRegularClassSymbol(session)?.serializableWith

context(CheckerContext)
val ConeKotlinType.isGeneratedSerializableObject: Boolean
    get() = toRegularClassSymbol(session)?.let { it.classKind.isObject && it.hasSerializableOrMetaAnnotationWithoutArgs } ?: false

// ---------------------- other ----------------------

val CheckerContext.currentFile: FirFile
    get() = containingDeclarations.first() as FirFile
