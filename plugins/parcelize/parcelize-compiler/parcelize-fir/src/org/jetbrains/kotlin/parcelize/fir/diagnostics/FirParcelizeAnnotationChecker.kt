/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parcelize.ParcelizeNames.DEPRECATED_RUNTIME_PACKAGE
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELIZE_CLASS_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.RAW_VALUE_ANNOTATION_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.TYPE_PARCELER_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_WITH_CLASS_IDS

object FirParcelizeAnnotationChecker : FirAnnotationCallChecker() {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotationType = expression.annotationTypeRef.coneType.fullyExpandedType(context.session) as? ConeClassLikeType ?: return
        val resolvedAnnotationSymbol = annotationType.lookupTag.toFirRegularClassSymbol(context.session) ?: return
        when (val annotationClassId = resolvedAnnotationSymbol.classId) {
            in TYPE_PARCELER_CLASS_IDS -> {
                checkTypeParcelerUsage(expression, context, reporter)
                checkDeprecatedAnnotations(expression, annotationClassId, context, reporter, isForbidden = true)
            }
            in WRITE_WITH_CLASS_IDS -> {
                checkWriteWithUsage(expression, context, reporter)
                checkDeprecatedAnnotations(expression, annotationClassId, context, reporter, isForbidden = true)
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
    ) {
        if (annotationClassId.packageFqName == DEPRECATED_RUNTIME_PACKAGE) {
            val factory = if (isForbidden) KtErrorsParcelize.FORBIDDEN_DEPRECATED_ANNOTATION else KtErrorsParcelize.DEPRECATED_ANNOTATION
            reporter.reportOn(annotationCall.source, factory, context)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkTypeParcelerUsage(annotationCall: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        // TODO: this check checks type arguments of annotation which are not supported in FIR
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkWriteWithUsage(annotationCall: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        // TODO: this check checks type arguments of annotation which are not supported in FIR
    }
}
