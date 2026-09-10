/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FirTypeRefSource
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.TRANSIENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.fir.services.findTypeSerializerOrContextUnchecked
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds

private val JAVA_SERIALIZABLE_ID = ClassId.topLevel(FqName("java.io.Serializable"))

internal fun CheckerContext.checkCorrectTransientAnnotationIsUsed(
    classSymbol: FirClassSymbol<*>,
    properties: List<FirSerializableProperty>,
    reporter: DiagnosticReporter
) {
    if (classSymbol.resolvedSuperTypes.any { it.classId == JAVA_SERIALIZABLE_ID }) return // do not check
    for (property in properties) {
        if (property.transient) continue
        val incorrectTransient =
            property.propertySymbol.backingFieldSymbol?.resolvedAnnotationsWithClassIds?.getAnnotationByClassId(
                TRANSIENT_ANNOTATION_CLASS_ID, session
            )
        if (incorrectTransient != null) {
            reporter.reportOn(
                source = incorrectTransient.source ?: property.propertySymbol.source,
                factory = FirSerializationErrors.INCORRECT_TRANSIENT
            )
        }
    }
}

internal fun CheckerContext.checkTransients(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
    for (propertySymbol in classSymbol.declaredProperties(session)) {
        val isInitialized = propertySymbol.isLateInit || declarationHasInitializer(propertySymbol)
        val transientAnnotation = propertySymbol.getSerialTransientAnnotation(session) ?: continue
        val hasBackingField = propertySymbol.hasBackingField
        if (!hasBackingField) {
            reporter.reportOn(transientAnnotation.source ?: propertySymbol.source, FirSerializationErrors.TRANSIENT_IS_REDUNDANT)
        } else if (!isInitialized) {
            reporter.reportOn(propertySymbol.source, FirSerializationErrors.TRANSIENT_MISSING_INITIALIZER)
        }
    }
}

private fun declarationHasInitializer(propertySymbol: FirPropertySymbol): Boolean {
    return when {
        propertySymbol.fromPrimaryConstructor -> propertySymbol.correspondingValueParameterFromPrimaryConstructor?.hasDefaultValue
            ?: false

        else -> propertySymbol.hasInitializer || propertySymbol.hasDelegate
    }
}

internal fun CheckerContext.analyzePropertiesSerializers(
    classSymbol: FirClassSymbol<*>,
    properties: List<FirSerializableProperty>,
    reporter: DiagnosticReporter
) {
    val classLookupTag = classSymbol.toLookupTag()
    for (property in properties) {
        // Don't report anything on properties from supertypes
        if (!classLookupTag.isRealOwnerOf(property.propertySymbol)) continue
        val customSerializerType = property.serializableWith
        val serializerSymbol = customSerializerType?.toRegularClassSymbol()
        val propertySymbol = property.propertySymbol
        val typeRef = propertySymbol.resolvedReturnTypeRef
        val propertyType = typeRef.coneType.fullyExpandedType()
        val source = typeRef.source ?: propertySymbol.source
        // There is no compile-time type to look a serializer up for, and @Contextual hides this from
        // SERIALIZER_NOT_FOUND, leaving the backend to fail on the cast to IrSimpleType. See KT-59088.
        if (propertyType is ConeDynamicType) {
            reporter.reportOn(source, FirSerializationErrors.DYNAMIC_TYPE_NOT_SUPPORTED)
            continue
        }
        if (customSerializerType != null && serializerSymbol != null) {
            // Do not account for @Polymorphic and @Contextual, as they are serializers for T: Any
            // and would not be compatible on direct comparison
            if (customSerializerType.classId in SerializersClassIds.setOfSpecialSerializers) return

            val serializerForType = customSerializerType.serializerForType(session)?.fullyExpandedType()

            checkCustomSerializerMatch(
                classSymbol,
                source = typeRef.source ?: propertySymbol.source,
                propertyType,
                customSerializerType,
                serializerForType,
                reporter
            )
            val annotationElement = propertySymbol.serializableAnnotation(needArguments = false, session)?.source
            checkCustomSerializerNotAbstract(classSymbol, source = annotationElement, customSerializerType, reporter)
            checkCustomSerializerIsNotLocal(source = annotationElement, classSymbol, customSerializerType, reporter)
            checkCustomSerializerParameters(
                classSymbol, annotationElement, propertyType, customSerializerType, serializerForType, typeRef, reporter
            )
            checkSerializerNullability(propertyType, customSerializerType, source, reporter)
        } else {
            checkType(typeRef, source, reporter)
            checkGenericArrayType(propertyType, source, reporter)
        }
    }
}

private fun CheckerContext.checkGenericArrayType(propertyType: ConeKotlinType, source: KtSourceElement?, reporter: DiagnosticReporter) {
    if (propertyType.isNonPrimitiveArray && propertyType.typeArguments.first().type?.isTypeParameter == true) {
        reporter.reportOn(
            source,
            FirSerializationErrors.GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED
        )
    }
}

private fun CheckerContext.checkTypeArguments(
    typeRef: FirTypeRef,
    fallbackSource: KtSourceElement?,
    reporter: DiagnosticReporter,
    skipStarProjections: Boolean = false,
) {
    val argsRefs = extractArgumentsTypeRefAndSource(typeRef, skipStarProjections) ?: return
    for (typeArgument in argsRefs) {
        val argTypeRef = typeArgument.typeRef ?: continue
        checkType(argTypeRef, typeArgument.source ?: fallbackSource, reporter)
    }
}

/**
 * Greatly simplified version of FirHelpers.extractArgumentsTypeRefAndSource:
 *
 * - Does not handle nested classes with type parameters (serialization does not support them)
 * - Does not handle anything other than FirUserTypeRef
 * - Replaces star projections with declaration-site upper bound if requested
 *      (K1 behavior that was adopted in serialization, see IrSimpleType.argumentTypesOrUpperBounds()/StarProjectionImpl.getType())
 *      Replacement not needed if we do not want to check them in [checkTypeArguments]
 */
private fun CheckerContext.extractArgumentsTypeRefAndSource(typeRef: FirTypeRef, skipStarProjections: Boolean): List<FirTypeRefSource>? {
    if (typeRef !is FirResolvedTypeRef) error("TypeRef should be already resolved in checker: ${typeRef.render()}")
    val result = mutableListOf<FirTypeRefSource>()
    when (val delegatedTypeRef = typeRef.delegatedTypeRef) {
        is FirUserTypeRef -> {
            val qualifier = delegatedTypeRef.qualifier.last()

            for ([index, typeArgument] in qualifier.typeArgumentList.typeArguments.withIndex()) {
                val ref = when (typeArgument) {
                    is FirTypeProjectionWithVariance -> typeArgument.typeRef
                    is FirStarProjection -> {
                        if (skipStarProjections) continue
                        val declarationClass =
                            typeRef.coneType.classSymbolOrUpperBound(session) ?: error("Not a class typeRef: ${typeRef.render()}")
                        declarationClass.typeParameterSymbols[index].resolvedBounds.first()
                    }
                    is FirPlaceholderProjection -> error("Should not be encountered in the property type")
                }
                result.add(FirTypeRefSource(ref, typeArgument.source))
            }
        }
        else -> return null
    }

    return result
}

private fun CheckerContext.checkType(typeRef: FirTypeRef, typeSource: KtSourceElement?, reporter: DiagnosticReporter) {
    val type = typeRef.coneType.fullyExpandedType()
    if (type.lowerBoundIfFlexible().isTypeParameter) return // type parameters always have serializer stored in class' field

    val serializer = findTypeSerializerOrContextUnchecked(type, this)
    if (serializer != null) {
        val classSymbol = type.toRegularClassSymbol() ?: return
        val customSerializerType = type.getSerializableWith(session)?.fullyExpandedType()
        if (customSerializerType != null) {
            val serializerForType = customSerializerType.serializerForType(session)?.fullyExpandedType()

            checkCustomSerializerMatch(classSymbol, typeSource, type, customSerializerType, serializerForType, reporter)
            checkCustomSerializerIsNotLocal(typeSource, classSymbol, customSerializerType, reporter)

            val annotationElement = type.customAnnotations.serializableAnnotation(session)?.source ?: typeSource
            checkCustomSerializerParameters(
                classSymbol, annotationElement, type, customSerializerType, serializerForType, typeRef, reporter
            )
            checkCustomSerializerNotAbstract(classSymbol, annotationElement, customSerializerType, reporter)
            checkSerializerNullability(type, customSerializerType, typeSource, reporter)
        } else {
            val allowStarProjections =
                serializer.classId == SerializersClassIds.sealedSerializerId || serializer.classId == SerializersClassIds.polymorphicSerializerId
            // For custom serializers, this check is performed in checkCustomSerializerParameters
            checkTypeArguments(typeRef, typeSource, reporter, allowStarProjections)
        }
    } else {
        if (type.classSymbolOrUpperBound(session)?.isEnumClass != true) {
            // enums are always serializable
            reporter.reportOn(typeSource, FirSerializationErrors.SERIALIZER_NOT_FOUND, type)
        }
    }
}

internal fun CheckerContext.checkCustomSerializerMatch(
    containingClassSymbol: FirClassSymbol<*>,
    source: KtSourceElement?,
    declarationType: ConeKotlinType,
    serializerType: ConeKotlinType,
    serializerForType: ConeKotlinType?,
    reporter: DiagnosticReporter
) {
    serializerForType ?: return

    val declarationTypeClassId = declarationType.classId
    if (declarationTypeClassId == null || declarationTypeClassId != serializerForType.classId) {
        reporter.reportOn(
            source ?: containingClassSymbol.serializableOrMetaAnnotationSource(session),
            FirSerializationErrors.SERIALIZER_TYPE_INCOMPATIBLE,
            declarationType,
            serializerType,
            serializerForType
        )
    }
}

internal fun CheckerContext.checkCustomSerializerNotAbstract(
    containingClassSymbol: FirClassSymbol<*>,
    source: KtSourceElement?,
    serializerType: ConeKotlinType,
    reporter: DiagnosticReporter,
) {
    if (serializerType.isAbstractOrSealedOrInterface(session)) {
        reporter.reportOn(
            source ?: containingClassSymbol.serializableOrMetaAnnotationSource(session),
            FirSerializationErrors.ABSTRACT_SERIALIZER_TYPE,
            containingClassSymbol.defaultType(),
            serializerType
        )
    }
}

internal fun CheckerContext.checkCustomSerializerParameters(
    containingClassSymbol: FirClassSymbol<*>,
    source: KtSourceElement?,
    declarationType: ConeKotlinType,
    serializerType: ConeKotlinType,
    serializerForType: ConeKotlinType?,
    useSiteType: FirTypeRef?,
    reporter: DiagnosticReporter,
) {
    serializerForType ?: return

    // Do not account for @Polymorphic and @Contextual, as they are serializers for T: Any
    // and would not be compatible on direct comparison
    if (serializerType.classId in SerializersClassIds.setOfSpecialSerializers) {
        return
    }

    val primaryConstructor = serializerType.toRegularClassSymbol()?.primaryConstructorIfAny(session) ?: return

    val targetElement by lazy { source ?: containingClassSymbol.serializableOrMetaAnnotationSource(session) }

    val isExternalSerializer = serializerType.toRegularClassSymbol()?.getSerializerAnnotation(session) != null

    val needArguments = primaryConstructor.valueParameterSymbols.isNotEmpty()
    // it is allowed that parameters are not passed in regular serializers at all
    if (!needArguments) return

    // The backend instantiates the custom serializer with one child serializer per type argument of the
    // *annotated declaration*, not of the type the serializer claims to serialize. These two differ whenever
    // the serializer is applied to a subtype — the case SERIALIZER_TYPE_INCOMPATIBLE merely warns about —
    // and looking at the wrong one used to let arity mismatches through to the backend, where the missing
    // argument crashed codegen. See KT-73207.
    val expectedParametersCount = declarationType.typeArguments.size

    if ( // for external serializer, the verification will be carried out at the definition
        !isExternalSerializer
        // if the parameters are still specified, then their number must match in the serializable class and constructor
        && expectedParametersCount != primaryConstructor.valueParameterSymbols.size
    ) {
        val message = if (expectedParametersCount > 0) {
            "expected no parameters or $expectedParametersCount, but has ${primaryConstructor.valueParameterSymbols.size} parameters"
        } else {
            "expected no parameters but has ${primaryConstructor.valueParameterSymbols.size} parameters"
        }
        reporter.reportOn(
            targetElement,
            FirSerializationErrors.CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT,
            serializerType,
            serializerForType,
            message
        )
    }

    if (useSiteType != null) {
        // Check that backend can in fact instantiate type arguments serializers to pass them to a custom serializer
        checkTypeArguments(useSiteType, targetElement, reporter)
    }

    primaryConstructor.valueParameterSymbols.forEach { param ->
        val returnType = param.resolvedReturnType
        if (!returnType.isKSerializer) {
            reporter.reportOn(
                targetElement,
                FirSerializationErrors.CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE,
                serializerType,
                serializerForType,
                param.name.asString()
            )
        }
    }
}

internal fun CheckerContext.checkCustomSerializerIsNotLocal(
    source: KtSourceElement?,
    classSymbol: FirClassSymbol<*>,
    serializerType: ConeKotlinType,
    reporter: DiagnosticReporter
) {
    if (serializerType.classLikeLookupTagIfAny?.toSymbol()?.isLocal == true) {
        reporter.reportOn(
            source ?: classSymbol.serializableOrMetaAnnotationSource(session),
            FirSerializationErrors.LOCAL_SERIALIZER_USAGE,
            serializerType
        )
    }
}

private fun CheckerContext.checkSerializerNullability(
    classType: ConeKotlinType,
    serializerType: ConeKotlinType,
    source: KtSourceElement?,
    reporter: DiagnosticReporter
) {
    // @Serializable annotation has proper signature so this error would be caught in type checker
    val serializerForType = serializerType.serializerForType(session) ?: return
    if (!classType.isMarkedNullable && serializerForType.isMarkedNullable) {
        reporter.reportOn(source, FirSerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE, serializerType, classType)
    }
}
