/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.InternalDiagnosticFactoryMethod
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.plugin.types.ConeNumberSignAttribute
import org.jetbrains.kotlin.fir.plugin.types.numberSign
import org.jetbrains.kotlin.fir.types.coneType

object SignedNumberCallChecker : FirFunctionCallChecker() {
    @OptIn(InternalDiagnosticFactoryMethod::class)
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val argumentMapping = expression.argumentMapping ?: return
        for ((argument, parameter) in argumentMapping.entries) {
            val expectedSign = parameter.returnTypeRef.coneType.attributes.numberSign ?: continue
            val actualSign = argument.typeRef.coneType.attributes.numberSign
            if (expectedSign != actualSign) {
                withSuppressedDiagnostics(argument, context) {
                    reporter.reportOn(argument.source, AllOpenErrors.ILLEGAL_NUMBER_SIGN, expectedSign.asString(), actualSign.asString(), it)
                }
            }
        }
    }

    private fun ConeNumberSignAttribute?.asString(): String = when (this?.sign) {
        null -> "None"
        else -> sign.name
    }
}
