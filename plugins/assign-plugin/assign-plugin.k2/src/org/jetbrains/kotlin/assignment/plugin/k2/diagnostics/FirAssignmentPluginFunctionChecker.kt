/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2.diagnostics

import org.jetbrains.kotlin.assignment.plugin.k2.annotationMatchingService
import org.jetbrains.kotlin.assignment.plugin.k2.diagnostics.FirErrorsAssignmentPlugin.DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD

object FirAssignmentPluginFunctionChecker : FirSimpleFunctionChecker() {

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.origin != FirDeclarationOrigin.Source) return
        if (!declaration.isAssignMethod()) return

        val receiverClassSymbol = if (declaration.symbol.isExtension) {
            declaration.symbol.resolvedReceiverTypeRef?.toRegularClassSymbol(context.session)
        } else {
            declaration.dispatchReceiverType?.toRegularClassSymbol(context.session)
        }
        if (!context.session.annotationMatchingService.isAnnotated(receiverClassSymbol)) return
        if (!declaration.returnTypeRef.coneType.isUnit) {
            reporter.reportOn(declaration.source, DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT, context)
        }
    }

    private fun FirSimpleFunction.isAssignMethod(): Boolean {
        return valueParameters.size == 1 && this.name == ASSIGN_METHOD
    }
}
