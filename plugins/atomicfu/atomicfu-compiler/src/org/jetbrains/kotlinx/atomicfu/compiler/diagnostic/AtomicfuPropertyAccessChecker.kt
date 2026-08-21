/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.types.classId

/**
 * Checks that atomicfu properties are not used in a way unsupported by the compiler.
 *
 * Atomicfu plugin is extremely limited in what code patterns it supports.
 * And when there is something it does not support, it emits a cryptic error message that does not help
 * with finding the exact place where a "mistake" was made.
 *
 * This check addresses most common misuses.
 */
object AtomicfuPropertyAccessChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val property = expression.calleeReference.toResolvedPropertySymbol() ?: return
        if (property.isLocal) return
        if (property.resolvedReturnType.classId?.isAtomicType() != true) return

        if (expression.isLegalUsage()) return

        reporter.reportOn(expression.source, AtomicfuErrors.ATOMIC_PROPERTY_ACCESS_WITHOUT_OPERATION)
    }

    context(context: CheckerContext)
    private fun FirPropertyAccessExpression.isLegalUsage(): Boolean {
        var current: FirExpression = this
        for (index in context.containingElements.lastIndex - 1 downTo 0) {
            when (val parent = context.containingElements[index]) {
                // Delegate initialization (val a: Int by AtomicInt_property
                is FirProperty -> return parent.delegate === current
                // Only a left side of an assignment (one can initialize atomic property, but can not assign it elsewhere)
                is FirVariableAssignment -> return parent.lValue === current
                // It's totally legal to call a function or access a property on the atomic property
                is FirQualifiedAccessExpression -> return parent.explicitReceiver === current
                // If a safe cast was not rejected by other checker, it should be consumed
                is FirSafeCallExpression -> return parent.receiver === current
                // The property can only occur in cast's argument list and there it should be the only and only argument
                is FirArgumentList -> {
                    if (parent.arguments.singleOrNull() !== current) return false
                }
                // If there's a cast (whose legality is a different question), then the cast's result should be consumed
                is FirTypeOperatorCall, is FirCheckNotNullCall -> {
                    // FirArgumentList check above ensures that the property is an argument
                    current = parent
                }
                // The same (as for FirTypeOperatorCall) is true for smart casts
                is FirSmartCastExpression -> {
                    if (parent.originalExpression !== current) return false
                    current = parent
                }
                // Everything else is totally illegal
                else -> return false
            }
        }
        return false
    }
}
