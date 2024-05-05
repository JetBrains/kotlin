/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassLikeChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbAnnotations
import org.jetbrains.rhizomedb.fir.services.rhizomedbEntityPredicateMatcher

object RhizomedbFirEntityTypeChecker : FirClassLikeChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClassLikeDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass) return
        if (!declaration.hasAnnotation(RhizomedbAnnotations.generatedEntityTypeClassId, context.session)) return
        val pm = context.session.rhizomedbEntityPredicateMatcher
        if (!pm.isEntity(declaration.symbol)) {
            reporter.reportOn(declaration.source, RhizomedbFirErrors.NOT_ENTITY, context)
        }
    }
}