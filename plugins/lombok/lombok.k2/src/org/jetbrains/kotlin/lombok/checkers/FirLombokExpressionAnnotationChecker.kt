/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement

/**
 * Validates the Lombok annotations written on an expression (`val x = @ToString 1`).
 *
 * Such an annotation stays on the expression, so [FirLombokDeclarationAnnotationChecker] never sees it.
 * A lambda is the exception: its annotations end up on the `FirAnonymousFunction`, which is a declaration,
 * so the declaration checker already covers that case.
 */
object FirLombokExpressionAnnotationChecker : FirBasicExpressionChecker(MppCheckerKind.Platform) {
    private val expressionTargets = listOf(KotlinTarget.EXPRESSION)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        // Checked by FirLombokDeclarationAnnotationChecker, which knows the precise target of every declaration
        if (expression is FirDeclaration) return

        checkLombokAnnotations(expression.annotations, expressionTargets)
    }
}
