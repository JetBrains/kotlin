/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parcelize.ParcelizeNames.INTERNAL_PARCELABLE_ID

/**
 * Reports references to `kotlinx.parcelize.internal.Parcelable` in user code.
 *
 * Only the supertype generator of the plugin can use this type.
 * The generated supertypes have a fake source, so the checker does not report them.
 */
object FirParcelizeInternalParcelableTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val classId = typeRef.coneType.lowerBoundIfFlexible().abbreviatedTypeOrSelf.classId
        reportIfInternalParcelable(typeRef.source, classId)
    }
}

/**
 * Reports class literals and other qualifiers that refer to `kotlinx.parcelize.internal.Parcelable`, for example `Parcelable::class`.
 */
object FirParcelizeInternalParcelableQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        reportIfInternalParcelable(expression.source, expression.qualifierSymbol?.classId)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportIfInternalParcelable(source: KtSourceElement?, classId: ClassId?) {
    if (source == null || source.kind is KtFakeSourceElementKind) return
    if (classId != INTERNAL_PARCELABLE_ID) return
    reporter.reportOn(source, KtErrorsParcelize.INTERNAL_PARCELABLE_USAGE)
}
