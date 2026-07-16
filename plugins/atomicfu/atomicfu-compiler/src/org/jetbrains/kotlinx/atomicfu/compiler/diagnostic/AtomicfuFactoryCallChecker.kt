/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.text

/**
 * Checks that an atomicfu's atomic factory call is happened either to initialize a property,
 * or as a delegate expression after 'by'.
 */
object AtomicfuFactoryCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!expression.calleeReference.isAtomicFactory()) return

        val parentElement = context.containingElements.getOrNull(context.containingElements.lastIndex - 1) ?: return
        // Immediate property initialization or delegation
        if (expression.isPropertyInitializerOrDelegate(parentElement)) return
        // Relaxed initialization check: we only verify that the call is on the RHS of the assignment.
        // If the LHS does not make sense, other checkers should catch and report it.
        if (expression.isAssignmentRhs(parentElement)) return

        reporter.reportOn(
            expression.source,
            AtomicfuErrors.ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY,
            expression.source.text.toString()
        )
    }

    private fun FirFunctionCall.isPropertyInitializerOrDelegate(element: FirElement): Boolean {
        if (element !is FirProperty) return false
        return element.initializer === this || element.delegate === this
    }

    private fun FirFunctionCall.isAssignmentRhs(element: FirElement): Boolean {
        if (element !is FirVariableAssignment) return false
        return element.rValue == this
    }
}
