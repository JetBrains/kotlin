/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.rhizomedb.fir.resolve.KotlinStdlib
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbAnnotations

object RhizomedbFirPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasAnnotation(RhizomedbAnnotations.manyAnnotationClassId, context.session)) return
        if (!declaration.hasAnnotation(RhizomedbAnnotations.valueAttributeClassId, context.session) &&
            !declaration.hasAnnotation(RhizomedbAnnotations.referenceAttributeClassId, context.session)
        ) return
        if (declaration.returnTypeRef.isSet()) return
        reporter.reportOn(declaration.source, RhizomedbFirErrors.MANY_ATTRIBUTE_NOT_A_SET, context)
    }
}

private fun FirTypeRef.isSet(): Boolean {
    val type = (this as? FirResolvedTypeRef)?.type ?: return false
    return (type as? ConeClassLikeType)?.lookupTag?.classId == KotlinStdlib.setClassId && !type.isNullable
}