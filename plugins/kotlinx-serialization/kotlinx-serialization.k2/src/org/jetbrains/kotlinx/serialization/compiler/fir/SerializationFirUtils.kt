/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.deserialization.toQualifiedPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.inheritableSerialInfoClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.keepGeneratedSerializerAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.metaSerializableAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.polymorphicClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialInfoClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serialNameAnnotationClassId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationJsDependenciesClassIds

object AnnotationParameterNames {
    val VALUE = Name.identifier("value")
    val WITH = Name.identifier("with")
    val FOR_CLASS = Name.identifier("forClass")
}

// ---------------------- annotations utils ----------------------

fun FirBasedSymbol<*>.isSerialInfoAnnotation(session: FirSession): Boolean {
    return (hasAnnotation(serialInfoClassId, session)
            || hasAnnotation(inheritableSerialInfoClassId, session)
            || hasAnnotation(metaSerializableAnnotationClassId, session))
}

fun FirBasedSymbol<*>.isInheritableSerialInfoAnnotation(session: FirSession): Boolean =
    hasAnnotation(inheritableSerialInfoClassId, session)

fun FirBasedSymbol<*>.getSerialNameAnnotation(session: FirSession): FirAnnotation? =
    resolvedAnnotationsWithArguments.getAnnotationByClassId(serialNameAnnotationClassId, session)

fun FirBasedSymbol<*>.getSerialNameValue(session: FirSession): String? =
    getSerialNameAnnotation(session)?.getStringArgument(AnnotationParameterNames.VALUE, session)

fun FirBasedSymbol<*>.getSerialRequired(session: FirSession): Boolean =
    hasAnnotation(SerializationAnnotations.requiredAnnotationClassId, session)

fun FirBasedSymbol<*>.hasSerialTransient(session: FirSession): Boolean = getSerialTransientAnnotation(session) != null

fun FirBasedSymbol<*>.getSerialTransientAnnotation(session: FirSession): FirAnnotation? =
    getAnnotationByClassId(SerializationAnnotations.serialTransientClassId, session)

fun FirClassSymbol<*>.hasSerializableAnnotation(session: FirSession): Boolean {
    return serializableAnnotation(needArguments = false, session) != null
}

fun FirBasedSymbol<*>.serializableAnnotation(needArguments: Boolean, session: FirSession): FirAnnotation? {
    val annotations = if (needArguments) {
        resolvedAnnotationsWithArguments
    } else {
        resolvedCompilerAnnotationsWithClassIds
    }
    return annotations.serializableAnnotation(session)
}

fun List<FirAnnotation>.serializableAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(SerializationAnnotations.serializableAnnotationClassId, session)
}

fun FirClassSymbol<*>.hasSerializableAnnotationWithoutArgs(session: FirSession): Boolean =
    serializableAnnotation(needArguments = false, session)?.let {
        if (it is FirAnnotationCall) {
            it.arguments.isEmpty()
        } else {
            it.argumentMapping.mapping.isEmpty()
        }
    } ?: false

fun FirClassSymbol<*>.hasSerializableAnnotationWithArgs(session: FirSession): Boolean {
    val annotation = serializableAnnotation(needArguments = false, session) ?: return false
    return if (annotation is FirAnnotationCall) {
        annotation.arguments.isNotEmpty()
    } else {
        annotation.argumentMapping.mapping.isNotEmpty()
    }
}

internal fun FirBasedSymbol<*>.getSerializableWith(session: FirSession): ConeKotlinType? =
    serializableAnnotation(needArguments = true, session)?.getKClassArgument(AnnotationParameterNames.WITH, session)

internal fun List<FirAnnotation>.getSerializableWith(session: FirSession): ConeKotlinType? =
    serializableAnnotation(session)?.getKClassArgument(AnnotationParameterNames.WITH, session)

fun FirAnnotation.getGetKClassArgument(name: Name): FirGetClassCall? {
    return findArgumentByName(name) as? FirGetClassCall
}

internal fun FirClassSymbol<*>.getSerializerAnnotation(session: FirSession): FirAnnotation? =
    getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId, session)

// ---------------------- class utils ----------------------
internal fun FirClassSymbol<*>.getSerializerForClass(session: FirSession): ConeKotlinType? = resolvedAnnotationsWithArguments
    .getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId, session)
    ?.getKClassArgument(AnnotationParameterNames.FOR_CLASS, session)

internal fun FirClassLikeDeclaration.getSerializerFor(session: FirSession): FirGetClassCall? =
    getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId, session)
        ?.getGetKClassArgument(AnnotationParameterNames.FOR_CLASS)

internal fun FirClassSymbol<*>.isInternallySerializableObject(session: FirSession): Boolean =
    classKind.isObject && hasSerializableOrMetaAnnotationWithoutArgs(session)

internal fun FirClassSymbol<*>.isSerializableObject(session: FirSession): Boolean {
    return classKind.isObject && hasSerializableOrMetaAnnotation(session)
}

internal fun FirClassSymbol<*>.isSealedSerializableInterface(session: FirSession): Boolean =
    classKind.isInterface && rawStatus.modality == Modality.SEALED && hasSerializableOrMetaAnnotation(session)

internal fun FirClassSymbol<*>.isSerializableInterfaceWithCustom(session: FirSession): Boolean =
    classKind.isInterface && hasSerializableAnnotationWithArgs(session)

fun FirClassSymbol<*>.hasSerializableOrMetaAnnotation(session: FirSession): Boolean {
    return hasSerializableAnnotation(session) || hasMetaSerializableAnnotation(session)
}

fun FirClassSymbol<*>.hasMetaSerializableAnnotation(session: FirSession): Boolean {
    return session.predicateBasedProvider.matches(FirSerializationPredicates.hasMetaAnnotation, this)
}

internal fun FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion(session: FirSession): Boolean = isSerializableObject(session)
        || isSerializableEnum(session)
        || (classKind == ClassKind.CLASS && hasSerializableOrMetaAnnotation(session))
        || isSealedSerializableInterface(session)
        || isSerializableInterfaceWithCustom(session)

internal fun FirClassSymbol<*>.companionNeedsSerializerFactory(session: FirSession): Boolean {
    if (!moduleData.platform.run { isNative() || isJs() || isWasm() }) return false
    if (isSerializableObject(session)) return true
    if (isSerializableEnum(session)) return true
    if (isAbstractOrSealedSerializableClass(session)) return true
    if (isSealedSerializableInterface(session)) return true
    if (isSerializableInterfaceWithCustom(session)) return true
    if (typeParameterSymbols.isEmpty()) return false
    return true
}

internal fun FirClassSymbol<*>.isInternalSerializable(session: FirSession): Boolean {
    if (!classKind.isClass) return false
    return hasSerializableOrMetaAnnotationWithoutArgs(session)
}

/**
 * Internal serializer is a plugin generated serializer for final/open/abstract/sealed classes or factory serializer for enums.
 * A plugin generated serializer can be generated as main type serializer or kept serializer.
 */
internal fun FirClassSymbol<*>.shouldHaveInternalSerializer(session: FirSession): Boolean {
    return isInternalSerializable(session) || keepGeneratedSerializer(session)
}
internal fun FirClassSymbol<*>.shouldHaveGeneratedMethods(session: FirSession): Boolean {
    return isInternalSerializable(session)
            // in the version with the `keepGeneratedSerializer` annotation the enum factory is already present therefore
            // there is no need to generate additional methods
            || (keepGeneratedSerializer(session) && !classKind.isEnumClass && !classKind.isObject)
}

// TODO: rewrite me according to phase contracts (KT-76122)
@OptIn(SymbolInternals::class)
internal fun FirClassSymbol<*>.keepGeneratedSerializer(session: FirSession): Boolean {
    return annotations.getAnnotationByClassId(
        keepGeneratedSerializerAnnotationClassId,
        session
    ) != null
}

internal fun FirClassSymbol<*>.hasPolymorphicAnnotation(session: FirSession): Boolean {
    return resolvedAnnotationsWithClassIds.getAnnotationByClassId(
        polymorphicClassId,
        session
    ) != null
}

fun FirClassSymbol<*>.hasSerializableOrMetaAnnotationWithoutArgs(session: FirSession): Boolean {
    return hasSerializableAnnotationWithoutArgs(session) ||
            (!hasSerializableAnnotation(session) && hasMetaSerializableAnnotation(session))
}

internal fun FirClassSymbol<*>.isAbstractOrSealedSerializableClass(session: FirSession): Boolean =
    isInternalSerializable(session) && (rawStatus.modality == Modality.ABSTRACT || rawStatus.modality == Modality.SEALED)

/**
 * Check that class is enum and marked by `Serializable` or meta-serializable annotation.
 */
internal fun FirClassSymbol<*>.isSerializableEnum(session: FirSession): Boolean {
    return classKind.isEnumClass && hasSerializableOrMetaAnnotation(session)
}

internal fun FirClassSymbol<*>.isFinalOrOpen(): Boolean {
    val modality = rawStatus.modality
    // null means default modality, final
    return (modality == null || modality == Modality.FINAL || modality == Modality.OPEN)
}

fun FirClassSymbol<*>.isEnumWithLegacyGeneratedSerializer(session: FirSession): Boolean =
    classKind.isEnumClass &&
            session.dependencySerializationInfoProvider.useGeneratedEnumSerializer &&
            hasSerializableOrMetaAnnotationWithoutArgs(session)

fun FirClassSymbol<*>.shouldHaveGeneratedSerializer(session: FirSession): Boolean =
    (isInternalSerializable(session) && isFinalOrOpen())
            || isEnumWithLegacyGeneratedSerializer(session)
            // enum factory must be used for enums
            || (keepGeneratedSerializer(session) && !classKind.isEnumClass && !classKind.isObject)

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

fun ConeKotlinType.isGeneratedSerializableObject(session: FirSession): Boolean =
    toRegularClassSymbol(session)?.let { it.classKind.isObject && it.hasSerializableOrMetaAnnotationWithoutArgs(session) } ?: false

fun ConeKotlinType.isAbstractOrSealedOrInterface(session: FirSession): Boolean =
    toRegularClassSymbol(session)?.let { it.classKind.isInterface || it.rawStatus.modality == Modality.ABSTRACT || it.rawStatus.modality == Modality.SEALED }
        ?: false

fun ConeKotlinType.classSymbolOrUpperBound(session: FirSession): FirClassSymbol<*>? {
    return when (this) {
        is ConeSimpleKotlinType -> toClassSymbol(session)
        is ConeFlexibleType -> upperBound.toClassSymbol(session)
        is ConeDefinitelyNotNullType -> original.toClassSymbol(session)
    }
}

@DirectDeclarationsAccess
fun FirDeclaration.excludeFromJsExport(session: FirSession) {
    if (!session.moduleData.platform.isJs()) {
        return
    }
    val jsExportIgnore = session.symbolProvider.getClassLikeSymbolByClassId(SerializationJsDependenciesClassIds.jsExportIgnore)
    val jsExportIgnoreAnnotation = jsExportIgnore as? FirRegularClassSymbol ?: return
    val jsExportIgnoreConstructor = jsExportIgnoreAnnotation.declarationSymbols.firstIsInstanceOrNull<FirConstructorSymbol>() ?: return

    val jsExportIgnoreAnnotationCall = buildAnnotationCall {
        argumentList = FirEmptyArgumentList
        annotationTypeRef = buildResolvedTypeRef {
            coneType = jsExportIgnoreAnnotation.defaultType()
        }
        calleeReference = buildResolvedNamedReference {
            name = jsExportIgnoreAnnotation.name
            resolvedSymbol = jsExportIgnoreConstructor
        }

        containingDeclarationSymbol = this@excludeFromJsExport.symbol
    }

    replaceAnnotations(annotations + jsExportIgnoreAnnotationCall)
}

fun createDeprecatedHiddenAnnotation(session: FirSession): FirAnnotation = buildAnnotation {
    val deprecatedAnno =
        session.symbolProvider.getClassLikeSymbolByClassId(StandardClassIds.Annotations.Deprecated) as FirRegularClassSymbol

    annotationTypeRef = deprecatedAnno.defaultType().toFirResolvedTypeRef()

    argumentMapping = buildAnnotationArgumentMapping {
        mapping[Name.identifier("message")] = buildLiteralExpression(
            null,
            ConstantValueKind.String,
            "This synthesized declaration should not be used directly",
            setType = true
        )

        // It has nothing to do with enums deserialization, but it is simply easier to build it this way.
        mapping[Name.identifier("level")] = buildEnumEntryDeserializedAccessExpression {
            enumClassId = StandardClassIds.DeprecationLevel
            enumEntryName = Name.identifier("HIDDEN")
        }.toQualifiedPropertyAccessExpression(session)
    }
}

fun FirClassLikeDeclaration.markAsDeprecatedHidden(session: FirSession) {
    replaceAnnotations(annotations + listOf(createDeprecatedHiddenAnnotation(session)))
    replaceDeprecationsProvider(this.getDeprecationsProvider(session))
}
