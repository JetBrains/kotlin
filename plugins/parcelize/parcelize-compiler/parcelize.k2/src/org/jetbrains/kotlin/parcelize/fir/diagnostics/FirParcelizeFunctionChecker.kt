/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirParcelizeFunctionChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol(context.session)
        if (!containingClassSymbol.isParcelize(context.session)) return
        if (declaration.origin != FirDeclarationOrigin.Source) return
        if (declaration.isWriteToParcel() && declaration.isOverride) {
            reporter.reportOn(declaration.source, KtErrorsParcelize.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED, context)
        }
    }

    private fun FirSimpleFunction.isWriteToParcel(): Boolean {
        return typeParameters.isEmpty() &&
                valueParameters.size == 2 &&
                valueParameters[1].returnTypeRef.coneType.isInt &&
                returnTypeRef.coneType.isUnit
    }
}
