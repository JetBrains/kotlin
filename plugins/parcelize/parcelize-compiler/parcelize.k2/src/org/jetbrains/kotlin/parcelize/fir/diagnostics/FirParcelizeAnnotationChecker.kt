/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parcelize.ParcelizeNames
import org.jetbrains.kotlin.parcelize.ParcelizeNames.DEPRECATED_RUNTIME_PACKAGE
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELIZE_CLASS_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.RAW_VALUE_ANNOTATION_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.TYPE_PARCELER_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_WITH_CLASS_IDS

// TODO: extract common checker for expect interfaces
object FirParcelizeAnnotationChecker : FirAnnotationCallChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotationType = expression.annotationTypeRef.coneType.fullyExpandedType(context.session) as? ConeClassLikeType ?: return
        val resolvedAnnotationSymbol = annotationType.lookupTag.toFirRegularClassSymbol(context.session) ?: return
        when (val annotationClassId = resolvedAnnotationSymbol.classId) {
            in TYPE_PARCELER_CLASS_IDS -> {
                if (checkDeprecatedAnnotations(expression, annotationClassId, context, reporter, isForbidden = true)) {
                    checkTypeParcelerUsage(expression, context, reporter)
                }
            }
            in WRITE_WITH_CLASS_IDS -> {
                if (checkDeprecatedAnnotations(expression, annotationClassId, context, reporter, isForbidden = true)) {
                    checkWriteWithUsage(expression, context, reporter)
                }
            }
            in IGNORED_ON_PARCEL_CLASS_IDS -> {
                checkDeprecatedAnnotations(expression, annotationClassId, context, reporter, isForbidden = false)
            }
            in PARCELIZE_CLASS_CLASS_IDS, in RAW_VALUE_ANNOTATION_CLASS_IDS -> {
                checkDeprecatedAnnotations(expression, annotationClassId, context, reporter, isForbidden = false)
            }
        }
    }

    private fun checkDeprecatedAnnotations(
        annotationCall: FirAnnotationCall,
        annotationClassId: ClassId,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        isForbidden: Boolean
    ): Boolean {
        if (annotationClassId.packageFqName == DEPRECATED_RUNTIME_PACKAGE) {
            val factory = if (isForbidden) KtErrorsParcelize.FORBIDDEN_DEPRECATED_ANNOTATION else KtErrorsParcelize.DEPRECATED_ANNOTATION
            reporter.reportOn(annotationCall.source, factory, context)
            return false
        }
        return true
    }

    private fun checkTypeParcelerUsage(annotationCall: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val thisMappedType = annotationCall.typeArguments.takeIf { it.size == 2 }?.first()?.toConeTypeProjection()?.type
            ?: return

        val annotationContainer = context.annotationContainers.lastOrNull()
        val duplicatingAnnotationCount = annotationContainer
            ?.annotations
            ?.filter { it.toAnnotationClassId(context.session) in TYPE_PARCELER_CLASS_IDS }
            ?.mapNotNull { it.typeArguments.takeIf { it.size == 2 }?.first()?.toConeTypeProjection()?.type }
            ?.count { it == thisMappedType }

        if (duplicatingAnnotationCount != null && duplicatingAnnotationCount > 1) {
            val reportElement = annotationCall.typeArguments.firstOrNull()?.source ?: annotationCall.source
            reporter.reportOn(reportElement, KtErrorsParcelize.DUPLICATING_TYPE_PARCELERS, context)
            return
        }

        checkIfTheContainingClassIsParcelize(annotationCall, context, reporter)

        // If we are looking at a property defined in the primary constructor of a class, check that the
        // enclosing class doesn't have the same TypeParceler annotation.
        if (annotationContainer is FirProperty && annotationContainer.fromPrimaryConstructor == true) {
            val enclosingClass = context.findClosestClassOrObject() ?: return

            val annotationType = annotationCall.toAnnotationClassLikeType(context.session) ?: return
            if (checkForRedundantTypeParceler(enclosingClass, annotationType, context)) {
                val reportElement = annotationCall.calleeReference.source ?: annotationCall.source
                reporter.reportOn(reportElement, KtErrorsParcelize.REDUNDANT_TYPE_PARCELER, enclosingClass.symbol, context)
            }
        }
    }

    private fun checkForRedundantTypeParceler(
        enclosingClass: FirClass,
        annotationType: ConeClassLikeType,
        context: CheckerContext,
    ): Boolean {
        return enclosingClass.annotations
            .mapNotNull { it.toAnnotationClassLikeType(context.session) }
            .filter { it.classId == annotationType.classId && it.typeArguments.size == annotationType.typeArguments.size }
            .any {
                it.typeArguments.zip(annotationType.typeArguments)
                    .all { (first, second) -> first.type?.fullyExpandedType(context.session) == second.type?.fullyExpandedType(context.session) }
            }
    }

    private fun checkWriteWithUsage(annotationCall: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        checkIfTheContainingClassIsParcelize(annotationCall, context, reporter)

        // For `@WriteWith<P>` check that `P` is an object.
        val parcelerType = annotationCall.typeArguments.singleOrNull()?.toConeTypeProjection()?.type ?: return
        val parcelerTypeSymbol = parcelerType.toRegularClassSymbol(context.session)
        if (parcelerTypeSymbol?.classKind != ClassKind.OBJECT) {
            val reportElement = annotationCall.typeArguments.singleOrNull()?.source ?: annotationCall.source
            reporter.reportOn(reportElement, KtErrorsParcelize.PARCELER_SHOULD_BE_OBJECT, context)
        }

        // For `@WriteWith<P> T` where `P` is a subtype of `Parceler<E>`, check that T is a subtype of E.
        //
        // From the perspective of the `WriteWith` annotation call, `T` corresponds to the nearest enclosing annotation container
        // stripped of annotations.
        //
        // It's safe to assume that `Parceler` refers to `kotlinx.parcelize.Parceler` rather than `kotlinx.android.parcel.Parceler`,
        // since using the deprecated `WriteWith` annotation is an error.
        val targetType = (context.annotationContainers.lastOrNull() as? FirTypeRef)?.coneType
            ?.withAttributes(ConeAttributes.Empty) ?: return
        val parcelerSuperType = parcelerTypeSymbol?.getSuperTypes(context.session)
            ?.firstOrNull { it.classId == ParcelizeNames.PARCELER_ID } ?: return
        val expectedType = parcelerSuperType.typeArguments.singleOrNull()?.type
            ?.withAttributes(ConeAttributes.Empty) ?: return

        if (!targetType.isSubtypeOf(expectedType, context.session)) {
            val reportElement = annotationCall.typeArguments.singleOrNull()?.source ?: annotationCall.source
            reporter.reportOn(reportElement, KtErrorsParcelize.PARCELER_TYPE_INCOMPATIBLE, parcelerType, targetType, context)
        }
    }

    private fun checkIfTheContainingClassIsParcelize(annotationCall: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val enclosingClass = context.findClosestClassOrObject() ?: return

        if (!enclosingClass.symbol.isParcelize(context.session)) {
            val reportElement = annotationCall.calleeReference.source ?: annotationCall.source
            reporter.reportOn(reportElement, KtErrorsParcelize.CLASS_SHOULD_BE_PARCELIZE, enclosingClass.symbol, context)
        }
    }
}
