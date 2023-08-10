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
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.parcelize.BuiltinParcelableTypes
import org.jetbrains.kotlin.parcelize.ParcelizeNames
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_ID

object FirParcelizePropertyChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol(session) ?: return

        if (containingClassSymbol.isParcelize(session)) {
            val fromPrimaryConstructor = declaration.fromPrimaryConstructor ?: false
            if (
                !fromPrimaryConstructor &&
                (declaration.hasBackingField || declaration.delegate != null) &&
                !declaration.hasIgnoredOnParcel() &&
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
            if (outerClass != null && outerClass.symbol.isParcelize(session)) {
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
        val type = property.returnTypeRef.coneType
        if (type is ConeErrorType || containingClassSymbol.hasCustomParceler(context.session) || property.hasIgnoredOnParcel()) {
            return
        }

        val session = context.session
        val customParcelerTypes = getCustomParcelerTypes(property.annotations + containingClassSymbol.annotations, session)
        if (!checkParcelableType(type, customParcelerTypes, session)) {
            reporter.reportOn(property.returnTypeRef.source, KtErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED, context)
        }
    }

    private fun getCustomParcelerTypes(annotations: List<FirAnnotation>, session: FirSession): Set<ConeKotlinType> =
        annotations.mapNotNullTo(mutableSetOf()) { annotation ->
            if (annotation.fqName(session) in ParcelizeNames.TYPE_PARCELER_FQ_NAMES && annotation.typeArguments.size == 2) {
                annotation.typeArguments[0].toConeTypeProjection().type
            } else {
                null
            }
        }

    private fun checkParcelableType(type: ConeKotlinType, customParcelerTypes: Set<ConeKotlinType>, session: FirSession): Boolean {
        if (type.hasParcelerAnnotation(session) || type in customParcelerTypes) {
            return true
        }

        val upperBound = type.getErasedUpperBound(session)
        val symbol = upperBound?.toRegularClassSymbol(session)
            ?: return false

        if (symbol.classKind.isSingleton || symbol.classKind.isEnumClass) {
            return true
        }

        val fqName = symbol.classId.asFqNameString()
        if (fqName in BuiltinParcelableTypes.PARCELABLE_BASE_TYPE_FQNAMES) {
            return true
        }

        if (fqName in BuiltinParcelableTypes.PARCELABLE_CONTAINER_FQNAMES) {
            return upperBound.typeArguments.all { projection ->
                projection.type?.let { checkParcelableType(it, customParcelerTypes, session) } ?: false
            }
        }

        return with(session.typeContext) {
            type.anySuperTypeConstructor {
                it is ConeKotlinType &&
                        (it.classId?.asFqNameString() in BuiltinParcelableTypes.PARCELABLE_SUPERTYPE_FQNAMES ||
                                it.isSomeFunctionType(session))
            }
        }
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

    private fun ConeKotlinType.hasParcelerAnnotation(session: FirSession): Boolean {
        for (annotation in customAnnotations) {
            val fqName = annotation.fqName(session)
            if (fqName in ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES || fqName in ParcelizeNames.WRITE_WITH_FQ_NAMES) {
                return true
            }
        }
        return false
    }

    private fun FirProperty.hasIgnoredOnParcel(): Boolean {
        return annotations.hasIgnoredOnParcel() || (getter?.annotations?.hasIgnoredOnParcel() ?: false)
    }

    private fun List<FirAnnotation>.hasIgnoredOnParcel(): Boolean {
        return this.any {
            if (it.annotationTypeRef.coneType.classId !in IGNORED_ON_PARCEL_CLASS_IDS) return@any false
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
