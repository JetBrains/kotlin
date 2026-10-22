/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.typeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.parcelize.BuiltinParcelableTypes
import org.jetbrains.kotlin.parcelize.ParcelizeNames
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_ID

object FirParcelizePropertyChecker : FirPropertyChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val session = context.session
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol() ?: return

        if (containingClassSymbol.isParcelize(session)) {
            val fromPrimaryConstructor = declaration.fromPrimaryConstructor ?: false
            if (
                !fromPrimaryConstructor &&
                (declaration.hasBackingField || declaration.delegate != null) &&
                !declaration.hasIgnoredOnParcel(session) &&
                !containingClassSymbol.hasCustomParceler(session)
            ) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED)
            }
            if (fromPrimaryConstructor) {
                checkParcelableClassProperty(declaration, containingClassSymbol, context, reporter)
            }
        }

        if (declaration.name == CREATOR_NAME && containingClassSymbol.isCompanion && declaration.hasJvmFieldAnnotation(session)) {
            val outerClass = context.containingDeclarations.asReversed().getOrNull(1) as? FirRegularClassSymbol
            if (outerClass != null && outerClass.isParcelize(session)) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED)
            }
        }
    }

    private fun checkParcelableClassProperty(
        property: FirProperty,
        containingClassSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val session = context.session
        val type = property.returnTypeRef.coneType.fullyExpandedType(session)
        if (type is ConeErrorType || containingClassSymbol.hasCustomParceler(session) || property.hasIgnoredOnParcel(session)) {
            return
        }

        val customParcelerTypes = getCustomParcelerTypes(
            property.annotations + containingClassSymbol.resolvedAnnotationsWithClassIds, session
        )
        val unsupported = checkParcelableType(type, customParcelerTypes, context)
        val source = property.returnTypeRef.source
        if (UnsupportedType(type, UnsupportedType.Reason.NOT_SUPPORTED) in unsupported) {
            reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED, context)
            return
        }
        for (unsupportedType in unsupported) {
            when (unsupportedType.reason) {
                UnsupportedType.Reason.NOT_SUPPORTED ->
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED, unsupportedType.type, context)
                UnsupportedType.Reason.RECURSIVE_DATA_CLASS ->
                    reporter.reportOn(source,
                        KtErrorsParcelize.PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED,
                        unsupportedType.type,
                        context
                    )
            }
        }
    }

    private data class UnsupportedType(val type: ConeKotlinType, val reason: Reason) {
        enum class Reason {
            NOT_SUPPORTED, // The type cannot be serialized by 'Parcelize'.
            RECURSIVE_DATA_CLASS, // The type is a data class which (transitively) contains itself.
        }
    }

    private fun getCustomParcelerTypes(annotations: List<FirAnnotation>, session: FirSession): Set<ConeKotlinType> =
        annotations.mapNotNullTo(mutableSetOf()) { annotation ->
            val resolvedAnnotation = annotation.resolvedType.fullyExpandedType(session)
            if (annotation.fqName(session) in ParcelizeNames.TYPE_PARCELER_FQ_NAMES && resolvedAnnotation.typeArguments.size == 2) {
                resolvedAnnotation.typeArguments[0].type?.fullyExpandedType(session)
            } else {
                null
            }
        }

    // Returns the set of types that are *not* supported, together with the reason. This set can include types other than `type`
    // if it is a generally supported container type that contains unsupported elements in this instantiation.
    private fun checkParcelableType(
        type: ConeKotlinType,
        customParcelerTypes: Set<ConeKotlinType>,
        context: CheckerContext,
        inDataClass: Boolean = false,
        dataClassesInProgress: List<ConeClassLikeType> = emptyList(),
    ): Set<UnsupportedType> {
        val session = context.session
        if (
            type.hasParcelerAnnotation(session) ||
            type in customParcelerTypes ||
            (type.isMarkedNullable && type.withNullability(nullable = false, session.typeContext) in customParcelerTypes)
        ) {
            return emptySet()
        }

        val upperBound = type.getErasedUpperBound(session)
        val symbol = upperBound?.toRegularClassSymbol(session)
            ?: return setOf(UnsupportedType(type, UnsupportedType.Reason.NOT_SUPPORTED))

        if (symbol.classKind.isSingleton || symbol.classKind.isEnumClass) {
            return emptySet()
        }

        val fqName = symbol.classId.asFqNameString()
        if (fqName in BuiltinParcelableTypes.PARCELABLE_BASE_TYPE_FQNAMES) {
            return emptySet()
        }

        if (fqName in BuiltinParcelableTypes.PARCELABLE_CONTAINER_FQNAMES) {
            return upperBound.typeArguments.fold(emptySet()) { acc, arg ->
                val elementType = arg.type ?: StandardTypes.NullableAny
                acc union checkParcelableType(
                    elementType, customParcelerTypes, context, dataClassesInProgress = dataClassesInProgress,
                )
            }
        }

        if (type.anySuperTypeConstructor(session) { it.isParcelableSupertype(session) }) {
            return emptySet()
        }

        // A data class which occurs again while its own properties are being checked is recursive and cannot be serialized inline.
        if (symbol.isData) {
            val recursiveDataClass = dataClassesInProgress.firstOrNull { outer ->
                outer.lookupTag == upperBound.lookupTag && !outer.hasNestedTypeArgument(upperBound, session)
            }
            if (recursiveDataClass != null) {
                val recursiveDataClassType = recursiveDataClass
                    .withNullability(nullable = false, session.typeContext)
                    .withAttributes(ConeAttributes.Empty)
                return setOf(UnsupportedType(recursiveDataClassType, UnsupportedType.Reason.RECURSIVE_DATA_CLASS))
            }
        }

        if (symbol.isData && (inDataClass || type.customAnnotations.any { it.fqName(session) == ParcelizeNames.DATA_CLASS_ANNOTATION_FQ_NAME })) {
            val properties = symbol.declaredProperties(context.session).filter { it.fromPrimaryConstructor }
            // Serialization uses the property getters, deserialization uses the constructor.
            if (properties.any { !it.isVisible(context) } || symbol.primaryConstructorIfAny(session)?.isVisible(context) != true) {
                return setOf(UnsupportedType(type, UnsupportedType.Reason.NOT_SUPPORTED))
            }
            val typeMapping = symbol.typeParameterSymbols.zip(type.typeArguments).mapNotNull { [parameter, arg] ->
                when (arg) {
                    is ConeKotlinType -> parameter to arg
                    is ConeKotlinTypeProjectionOut -> parameter to arg.type
                    else -> null
                }
            }.toMap()
            val substitutor = substitutorByMap(typeMapping, context.session)
            return properties.fold(emptySet()) { acc, property ->
                val elementType = substitutor.substituteOrSelf(property.resolvedReturnType)
                acc union checkParcelableType(
                    elementType, customParcelerTypes, context, inDataClass = true,
                    dataClassesInProgress = dataClassesInProgress + upperBound,
                )
            }
        }

        if (type.anySuperTypeConstructor(session) { it.isSupportedSerializable() }) {
            return emptySet()
        }

        return setOf(UnsupportedType(type, UnsupportedType.Reason.NOT_SUPPORTED))
    }

    private fun ConeKotlinType.anySuperTypeConstructor(session: FirSession, predicate: (ConeKotlinType) -> Boolean): Boolean =
        with(session.typeContext) { anySuperTypeConstructor { it is ConeKotlinType && predicate(it) } }

    // Returns true if 'type' occurs, ignoring nullability, among the (possibly nested) type arguments of this type.
    private fun ConeKotlinType.hasNestedTypeArgument(type: ConeClassLikeType, session: FirSession): Boolean =
        typeArguments.any { argument ->
            val argumentType = argument.type?.fullyExpandedType(session) ?: return@any false
            (argumentType is ConeClassLikeType && argumentType.lookupTag == type.lookupTag &&
                    argumentType.typeArguments.contentEquals(type.typeArguments)) ||
                    argumentType.hasNestedTypeArgument(type, session)
        }

    private fun FirCallableSymbol<*>.isVisible(context: CheckerContext): Boolean {
        return context.session.visibilityChecker.isVisible(
            this,
            context.session,
            context.containingFileSymbol ?: return true,
            context.containingDeclarations,
            dispatchReceiver = null
        )
    }

    private fun ConeKotlinType.getErasedUpperBound(session: FirSession): ConeClassLikeType? =
        when (this) {
            is ConeClassLikeType ->
                fullyExpandedType(session)

            is ConeTypeParameterType -> {
                val bounds = lookupTag.typeParameterSymbol.resolvedBounds
                val representativeBound = bounds.firstOrNull {
                    val kind = it.coneType.toRegularClassSymbol(session)?.classKind
                        ?: return@firstOrNull false
                    kind != ClassKind.INTERFACE && kind != ClassKind.ANNOTATION_CLASS
                } ?: bounds.first()
                representativeBound.coneType.getErasedUpperBound(session)
            }

            else ->
                null
        }

    private fun ConeKotlinType.isParcelableSupertype(session: FirSession): Boolean =
        classId?.asFqNameString() in BuiltinParcelableTypes.PARCELABLE_SUPERTYPE_FQNAMES || isSomeFunctionType(session)

    private fun ConeKotlinType.isSupportedSerializable(): Boolean =
        classId?.asFqNameString() in BuiltinParcelableTypes.EXTERNAL_SERIALIZABLE_FQNAMES

    private fun ConeKotlinType.hasParcelerAnnotation(session: FirSession): Boolean {
        for (annotation in customAnnotations) {
            val fqName = annotation.fqName(session)
            if (fqName in ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES || fqName in ParcelizeNames.WRITE_WITH_FQ_NAMES) {
                return true
            }
        }
        return false
    }

    private fun FirProperty.hasIgnoredOnParcel(session: FirSession): Boolean {
        return annotations.hasIgnoredOnParcel(session) || (getter?.annotations?.hasIgnoredOnParcel(session) ?: false)
    }

    private fun List<FirAnnotation>.hasIgnoredOnParcel(session: FirSession): Boolean {
        return this.any {
            it.fqName(session) in IGNORED_ON_PARCEL_FQ_NAMES
        }
    }

    private fun FirRegularClassSymbol.hasCustomParceler(session: FirSession): Boolean {
        val companionObjectSymbol = this.resolvedCompanionObjectSymbol ?: return false
        return lookupSuperTypes(companionObjectSymbol, lookupInterfaces = true, deep = true, session).any {
            it.classId == PARCELER_ID
        }
    }
}
