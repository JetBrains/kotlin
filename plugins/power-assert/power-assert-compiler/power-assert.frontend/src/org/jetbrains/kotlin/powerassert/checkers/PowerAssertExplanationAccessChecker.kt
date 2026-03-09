/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.powerassert.PowerAssertDiagnostics

internal object PowerAssertExplanationAccessChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    private val powerAssertClassId: ClassId =
        ClassId(FqName("kotlinx.powerassert"), Name.identifier("PowerAssert"))

    private val powerAssertExplanationCallableId: CallableId =
        CallableId(powerAssertClassId.createNestedClassId(Name.identifier("Companion")), callableName = Name.identifier("explanation"))

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        // Only check calls to 'PowerAssert.explanation'.
        if (expression.toResolvedCallableSymbol()?.callableId != powerAssertExplanationCallableId) return

        // One of the containers is annotated with '@PowerAssert'.
        if (context.containingDeclarations.none { it.hasAnnotation(powerAssertClassId, context.session) }) {
            reporter.reportOn(
                expression.source,
                PowerAssertDiagnostics.POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS,
            )
        }
    }
}
