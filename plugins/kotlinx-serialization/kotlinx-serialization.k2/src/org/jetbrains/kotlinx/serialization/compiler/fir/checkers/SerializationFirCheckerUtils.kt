/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SpecialBuiltins

// ---------------------- search utils ----------------------

internal fun FirClassSymbol<*>?.classSerializer(c: CheckerContext): FirClassSymbol<*>? {
    if (this == null) return null
    val session = c.session
    // serializer annotation on class?
    getSerializableWith(session)?.let { return it.classSymbolOrUpperBound(session) }
    // companion object serializer?
    if (this is FirRegularClassSymbol && isInternallySerializableObject(session)) return companionObjectSymbol
    // can infer @Poly?
    polymorphicSerializerIfApplicableAutomatically(session)?.let { return it }
    // default serializable?
    if (shouldHaveGeneratedSerializer(session)) {
        // $serializer nested class
        return unsubstitutedScope(c)
            .getSingleClassifier(SerialEntityNames.SERIALIZER_CLASS_NAME) as? FirClassSymbol<*>
    }
    return null
}

fun FirClassSymbol<*>.polymorphicSerializerIfApplicableAutomatically(session: FirSession): FirClassSymbol<*>? {
    val serializerName = when {
        isInterface -> when (modality) {
            Modality.SEALED -> SpecialBuiltins.sealedSerializer
            else -> SpecialBuiltins.polymorphicSerializer
        }

        isInternalSerializable(session) -> when (modality) {
            Modality.SEALED -> SpecialBuiltins.sealedSerializer
            Modality.ABSTRACT -> SpecialBuiltins.polymorphicSerializer
            else -> null
        }

        else -> null
    }
    return serializerName?.let { session.dependencySerializationInfoProvider.getClassFromSerializationPackage(Name.identifier(it)) }
}

// ---------------------- annotation utils ----------------------

internal fun FirAnnotation.getAnnotationClassSymbol(session: FirSession): FirRegularClassSymbol? = annotationTypeRef.coneType
    .fullyExpandedType(session)
    .toRegularClassSymbol(session)

internal fun FirAnnotation.isMetaSerializableAnnotation(session: FirSession): Boolean =
    getAnnotationClassSymbol(session)?.hasAnnotation(SerializationAnnotations.metaSerializableAnnotationClassId, session) ?: false


internal fun FirClassSymbol<*>.metaSerializableAnnotation(session: FirSession, needArguments: Boolean): FirAnnotation? {
    val annotations = if (needArguments) resolvedAnnotationsWithClassIds else resolvedAnnotationsWithArguments
    return annotations.firstOrNull { it.isMetaSerializableAnnotation(session) }
}

internal fun FirClassSymbol<*>.serializableOrMetaAnnotationSource(session: FirSession): KtSourceElement? {
    serializableAnnotation(needArguments = false, session)?.source?.let { return it }
    metaSerializableAnnotation(session, needArguments = false)?.source?.let { return it }
    return null
}

internal fun FirBasedSymbol<*>.hasAnySerialAnnotation(session: FirSession): Boolean =
    getSerialNameValue(session) != null || resolvedAnnotationsWithClassIds.any {
        it.getAnnotationClassSymbol(session)?.isSerialInfoAnnotation(session) == true
    }

// ---------------------- class utils ----------------------

fun FirClassSymbol<*>.superClassNotAny(session: FirSession): FirRegularClassSymbol? {
    return superClassOrAny(session).takeUnless { it.classId == StandardClassIds.Any }
}

fun FirClassSymbol<*>.superClassOrAny(session: FirSession): FirRegularClassSymbol {
    return resolvedSuperTypes.firstNotNullOfOrNull { superType ->
        superType.fullyExpandedType(session)
            .toRegularClassSymbol(session)
            ?.takeIf { it.classKind == ClassKind.CLASS }
    } ?: session.builtinTypes.anyType.toRegularClassSymbol(session) ?: error("Symbol for kotlin/Any not found")
}

internal fun FirClassSymbol<*>.isSerializableEnumWithMissingSerializer(session: FirSession): Boolean {
    if (!isEnumClass) return false
    if (hasSerializableOrMetaAnnotation(session)) return false
    if (hasAnySerialAnnotation(session)) return true
    return collectEnumEntries().any { it.hasAnySerialAnnotation(session) }
}

internal fun FirClassSymbol<*>.serializableAnnotationIsUseless(session: FirSession): Boolean = !classKind.isEnumClass &&
        hasSerializableOrMetaAnnotationWithoutArgs(session) &&
        !isInternalSerializable(session) &&
        !isInternallySerializableObject(session) &&
        !isSealedSerializableInterface(session)

// ---------------------- type utils ----------------------

internal fun ConeKotlinType.getSerializableWith(session: FirSession): ConeKotlinType? =
    customAnnotations.getSerializableWith(session) ?: toRegularClassSymbol(session)?.getSerializableWith(session)


internal fun ConeKotlinType.getOverriddenSerializer(session: FirSession): ConeKotlinType? =
    toRegularClassSymbol(session)?.getSerializableWith(session)


// ---------------------- others ----------------------
internal val CheckerContext.currentFile: FirFile
    get() = containingDeclarations.first() as FirFile
