/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parcelize.BuiltinParcelableTypes
import org.jetbrains.kotlin.parcelize.ParcelizeNames
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_ID

class FirParcelizePropertyChecker(private val parcelizeAnnotations: List<ClassId>) : FirPropertyChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol(session) ?: return

        if (containingClassSymbol.isParcelize(session, parcelizeAnnotations)) {
            val fromPrimaryConstructor = declaration.fromPrimaryConstructor ?: false
            if (
                !fromPrimaryConstructor &&
                (declaration.hasBackingField || declaration.delegate != null) &&
                !declaration.hasIgnoredOnParcel(session) &&
                !containingClassSymbol.hasCustomParceler(session)
            ) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED, context)
            }
            if (fromPrimaryConstructor) {
                checkParcelableClassProperty(declaration, containingClassSymbol, context, reporter)
            }
        }

        if (declaration.name == CREATOR_NAME && containingClassSymbol.isCompanion && declaration.hasJvmFieldAnnotation(session)) {
            val outerClass = context.containingDeclarations.asReversed().getOrNull(1) as? FirRegularClass
            if (outerClass != null && outerClass.symbol.isParcelize(session, parcelizeAnnotations)) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED, context)
            }
        }
    }

    private fun checkParcelableClassProperty(
        property: FirProperty,
        containingClassSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
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
        if (type in unsupported) {
            reporter.reportOn(property.returnTypeRef.source, KtErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED, context)
        } else {
            unsupported.forEach {
                reporter.reportOn(property.returnTypeRef.source, KtErrorsParcelize.PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED, it, context)
            }
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

    // Returns the set of types that are *not* supported. This set can include types other than `type`
    // if it is a generally supported container type that contains unsupported elements in this instantiation.
    private fun checkParcelableType(
        type: ConeKotlinType,
        customParcelerTypes: Set<ConeKotlinType>,
        context: CheckerContext,
        inDataClass: Boolean = false
    ): Set<ConeKotlinType> {
        val session = context.session
        if (type.hasParcelerAnnotation(session) || type in customParcelerTypes) {
            return emptySet()
        }

        val upperBound = type.getErasedUpperBound(session)
        val symbol = upperBound?.toRegularClassSymbol(session)
            ?: return setOf(type)

        if (symbol.classKind.isSingleton || symbol.classKind.isEnumClass) {
            return emptySet()
        }

        val fqName = symbol.classId.asFqNameString()
        if (fqName in BuiltinParcelableTypes.PARCELABLE_BASE_TYPE_FQNAMES) {
            return emptySet()
        }

        if (fqName in BuiltinParcelableTypes.PARCELABLE_CONTAINER_FQNAMES) {
            return upperBound.typeArguments.fold(emptySet()) { acc, arg ->
                val elementType = arg.type ?: session.builtinTypes.nullableAnyType.coneType
                acc union checkParcelableType(elementType, customParcelerTypes, context)
            }
        }

        if (type.anySuperTypeConstructor(session) { it.isParcelableSupertype(session) }) {
            return emptySet()
        }

        if (symbol.isData && (inDataClass || type.customAnnotations.any { it.fqName(session) == ParcelizeNames.DATA_CLASS_ANNOTATION_FQ_NAME })) {
            val properties = symbol.declaredProperties(context.session).filter { it.fromPrimaryConstructor }
            // Serialization uses the property getters, deserialization uses the constructor.
            if (properties.any { !it.isVisible(context) } || symbol.primaryConstructorIfAny(session)?.isVisible(context) != true) {
                return setOf(type)
            }
            val typeMapping = symbol.typeParameterSymbols.zip(type.typeArguments).mapNotNull { (parameter, arg) ->
                when (arg) {
                    is ConeKotlinType -> parameter to arg
                    is ConeKotlinTypeProjectionOut -> parameter to arg.type
                    else -> null
                }
            }.toMap()
            val substitutor = substitutorByMap(typeMapping, context.session)
            return properties.fold(emptySet()) { acc, property ->
                val elementType = substitutor.substituteOrSelf(property.resolvedReturnType)
                acc union checkParcelableType(elementType, customParcelerTypes, context, inDataClass = true)
            }
        }

        if (type.anySuperTypeConstructor(session) { it.isSupportedSerializable() }) {
            return emptySet()
        }

        return setOf(type)
    }

    private fun ConeKotlinType.anySuperTypeConstructor(session: FirSession, predicate: (ConeKotlinType) -> Boolean): Boolean =
        with(session.typeContext) { anySuperTypeConstructor { it is ConeKotlinType && predicate(it) } }

    @OptIn(SymbolInternals::class)
    private fun FirCallableSymbol<*>.isVisible(context: CheckerContext): Boolean {
        return context.session.visibilityChecker.isVisible(
            fir,
            context.session,
            context.containingFile ?: return true,
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
            if (it.fqName(session) !in IGNORED_ON_PARCEL_FQ_NAMES) return@any false
            val target = it.useSiteTarget
            target == null || target == AnnotationUseSiteTarget.PROPERTY || target == AnnotationUseSiteTarget.PROPERTY_GETTER
        }
    }

    private fun FirRegularClassSymbol.hasCustomParceler(session: FirSession): Boolean {
        val companionObjectSymbol = this.companionObjectSymbol ?: return false
        return lookupSuperTypes(companionObjectSymbol, lookupInterfaces = true, deep = true, session).any {
            it.classId == PARCELER_ID
        }
    }
}
