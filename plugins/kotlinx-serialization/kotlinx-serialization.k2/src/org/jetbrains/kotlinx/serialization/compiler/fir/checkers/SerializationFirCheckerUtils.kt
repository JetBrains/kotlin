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
import org.jetbrains.kotlinx.serialization.compiler.fir.serializableWith
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SpecialBuiltins

// ---------------------- search utils ----------------------

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>?.classSerializer: FirClassSymbol<*>?
    get() {
        if (this == null) return null
        // serializer annotation on class?
        serializableWith?.let { return it.toRegularClassSymbol(session) }
        // companion object serializer?
        if (this is FirRegularClassSymbol && with(session) { isInternallySerializableObject }) return companionObjectSymbol
        // can infer @Poly?
        polymorphicSerializerIfApplicableAutomatically?.let { return it }
        // default serializable?
        if (with(session) { shouldHaveGeneratedSerializer }) {
            // $serializer nested class
            return unsubstitutedScope(this@CheckerContext)
                .getSingleClassifier(SerialEntityNames.SERIALIZER_CLASS_NAME) as? FirClassSymbol<*>
        }
        return null
    }

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
val FirClassSymbol<*>.polymorphicSerializerIfApplicableAutomatically: FirClassSymbol<*>?
    get() {
        val serializerName = when {
            isInterface -> when (modality) {
                Modality.SEALED -> SpecialBuiltins.sealedSerializer
                else -> SpecialBuiltins.polymorphicSerializer
            }

            with(session) { isInternalSerializable } -> when (modality) {
                Modality.SEALED -> SpecialBuiltins.sealedSerializer
                Modality.ABSTRACT -> SpecialBuiltins.polymorphicSerializer
                else -> null
            }

            else -> null
        }
        return serializerName?.let { session.dependencySerializationInfoProvider.getClassFromSerializationPackage(Name.identifier(it)) }
    }

// ---------------------- annotation utils ----------------------

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirAnnotation.annotationClassSymbol: FirRegularClassSymbol?
    get() = annotationTypeRef.coneType
        .fullyExpandedType(session)
        .toRegularClassSymbol(session)

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirAnnotation.isMetaSerializableAnnotation: Boolean
    get() = annotationClassSymbol?.hasAnnotation(SerializationAnnotations.metaSerializableAnnotationClassId) ?: false


context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal fun FirClassSymbol<*>.metaSerializableAnnotation(needArguments: Boolean): FirAnnotation? {
    val annotations = if (needArguments) resolvedAnnotationsWithClassIds else resolvedAnnotationsWithArguments
    return annotations.firstOrNull { it.isMetaSerializableAnnotation }
}

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.serializableOrMetaAnnotationSource: KtSourceElement?
    get() {
        serializableAnnotation(needArguments = false)?.source?.let { return it }
        metaSerializableAnnotation(needArguments = false)?.source?.let { return it }
        return null
    }

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirBasedSymbol<*>.hasAnySerialAnnotation: Boolean
    get() = serialNameValue != null || resolvedAnnotationsWithClassIds.any { it.annotationClassSymbol?.isSerialInfoAnnotation == true }

// ---------------------- class utils ----------------------

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
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.isSerializableEnumWithMissingSerializer: Boolean
    get() {
        if (!isEnumClass) return false
        if (with(session) { hasSerializableOrMetaAnnotation }) return false
        if (hasAnySerialAnnotation) return true
        return collectEnumEntries().any { it.hasAnySerialAnnotation }
    }

context(FirSession)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val FirClassSymbol<*>.serializableAnnotationIsUseless: Boolean
    get() = !classKind.isEnumClass &&
            hasSerializableOrMetaAnnotationWithoutArgs &&
            !isInternalSerializable &&
            !isInternallySerializableObject &&
            !isSealedSerializableInterface

// ---------------------- type utils ----------------------

context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val ConeKotlinType.serializableWith: ConeKotlinType?
    get() = customAnnotations.serializableWith ?: toRegularClassSymbol(session)?.serializableWith


context(CheckerContext)
@Suppress("IncorrectFormatting") // KTIJ-22227
internal val ConeKotlinType.overriddenSerializer: ConeKotlinType?
    get() = toRegularClassSymbol(session)?.serializableWith


// ---------------------- others ----------------------
internal val CheckerContext.currentFile: FirFile
    get() = containingDeclarations.first() as FirFile
