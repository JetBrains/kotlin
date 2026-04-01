/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.powerassert.PowerAssertDiagnostics
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_EXPLANATION_CALLABLE_ID

internal object PowerAssertExplanationAccessChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        // Only check calls to 'PowerAssert.explanation'.
        if (expression.toResolvedCallableSymbol()?.callableId != POWER_ASSERT_EXPLANATION_CALLABLE_ID) return

        // One of the containers is annotated with '@PowerAssert'.
        if (context.containingDeclarations.none { it.isPowerAssertFunction() }) {
            reporter.reportOn(
                expression.source,
                PowerAssertDiagnostics.POWER_ASSERT_ILLEGAL_EXPLANATION_ACCESS,
            )
        }
    }
}
