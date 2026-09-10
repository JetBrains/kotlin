/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.secondToLastContainer
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * Checks that an atomicfu's atomic factory call happens either to initialize a property,
 * or as a delegate expression after 'by'.
 */
object AtomicfuFactoryCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (!expression.calleeReference.isAtomicFactory()) return

        val parentElement = context.secondToLastContainer ?: return
        if (parentElement is FirProperty) {
            // Immediate property initialization or delegation
            val isValidInitialization =
                parentElement.initializer == expression && parentElement.symbol.resolvedReturnType.classId?.isAtomicType() == true
            if (isValidInitialization) return
            if (parentElement.delegate == expression) return
        } else if (parentElement is FirVariableAssignment) {
            // Relaxed initialization check: we only verify that the call is on the RHS of the assignment.
            // If the LHS does not make sense, other checkers should catch and report it.
            if (parentElement.rValue == expression && parentElement.lValue.resolvedType.classId?.isAtomicType() == true) {
                return // legal
            }
        }

        reporter.reportOn(expression.source, AtomicfuErrors.ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY)
    }
}
