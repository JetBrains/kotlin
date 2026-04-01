/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.powerassert.PowerAssertDiagnostics
import org.jetbrains.kotlin.powerassert.PowerAssertNames
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

object PowerAssertAnnotationChecker : FirAnnotationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirAnnotation) {
        if (expression.toAnnotationClassId(context.session) != PowerAssertNames.POWER_ASSERT_CLASS_ID) return

        // Inapplicable target or other errors should be reported if either of these calculations are 'null'.
        val container = context.containingElements.dropLast(1).lastIsInstanceOrNull<FirAnnotationContainer>() ?: return
        val function = container as? FirNamedFunction ?: return

        // The function may be annotated with '@PowerAssert',
        // but check that it is actually a Power-Assert function.
        // TODO(KT-85237): Forbid annotation on only 'expect' function.
        if (!function.symbol.isPowerAssertFunction()) {
            reporter.reportOn(
                source = expression.source,
                factory = when {
                    function.isOverride -> PowerAssertDiagnostics.POWER_ASSERT_ILLEGAL_OVERRIDE
                    function.isActual -> PowerAssertDiagnostics.POWER_ASSERT_ILLEGAL_ACTUAL
                    else -> errorWithAttachment("Unexpected function: $function") {
                        withFirEntry("function", function)
                    }
                }
            )
        }
    }
}
