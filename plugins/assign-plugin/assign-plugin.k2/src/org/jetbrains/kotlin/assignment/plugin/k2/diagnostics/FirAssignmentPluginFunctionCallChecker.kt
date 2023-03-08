/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2.diagnostics

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.assignment.plugin.k2.annotationMatchingService
import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin.CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin.NO_APPLICABLE_ASSIGN_METHOD
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSingleCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD

object FirAssignmentPluginFunctionCallChecker : FirFunctionCallChecker() {

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!expression.isOverloadAssignCallCandidate()) return

        val calleeReference = expression.calleeReference
        if (calleeReference.isError()) {
            if (expression.isOverloadedAssignCallError(context.session, calleeReference.diagnostic)) {
                reporter.reportOn(expression.source, NO_APPLICABLE_ASSIGN_METHOD, context)
            }
        } else if (expression.isOverloadedAssignCall(context.session) && !expression.isReturnTypeUnit()) {
            reporter.reportOn(expression.source, CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT, context)
        }
    }

    private fun FirFunctionCall.isOverloadAssignCallCandidate() =
        arguments.size == 1 && source?.kind == KtFakeSourceElementKind.DesugaredCompoundAssignment

    private fun FirFunctionCall.isOverloadedAssignCallError(session: FirSession, diagnostic: ConeDiagnostic): Boolean {
        val functionName = when (diagnostic) {
            is ConeAmbiguityError -> diagnostic.name
            is ConeDiagnosticWithSingleCandidate -> diagnostic.candidate.callInfo.name
            is ConeUnresolvedNameError -> diagnostic.name
            else -> calleeReference.name
        }
        return functionName == ASSIGN_METHOD && isAnnotated(session)
    }

    private fun FirFunctionCall.isOverloadedAssignCall(session: FirSession) =
        calleeReference.name == ASSIGN_METHOD && isAnnotated(session)

    private fun FirFunctionCall.isAnnotated(session: FirSession): Boolean =
        session.annotationMatchingService.isAnnotated(explicitReceiver?.typeRef?.toRegularClassSymbol(session))

    private fun FirFunctionCall.isReturnTypeUnit() = toResolvedCallableSymbol()?.resolvedReturnType?.isUnit ?: false
}
