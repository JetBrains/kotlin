/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.powerassert.PowerAssertDiagnostics
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_CLASS_ID

internal object PowerAssertRuntimeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val function = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        if (!function.hasAnnotation(POWER_ASSERT_CLASS_ID, context.session)) return
        if (POWER_ASSERT_CLASS_ID.toSymbol() == null) {
            reporter.reportOn(
                expression.source,
                PowerAssertDiagnostics.POWER_ASSERT_RUNTIME_UNAVAILABLE,
            )
        }
    }
}
