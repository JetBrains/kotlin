/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds

object AtomicfuIllegalFunctionCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.dispatchReceiver?.resolvedType?.classId?.isAtomicType() != true) return
        val callable = expression.toResolvedCallableSymbol()?.callableId ?: return
        val name = callable.callableName
        val classId = callable.classId
        if (classId == StandardClassIds.Any) {
            if (name != StandardNames.EQUALS_NAME && name != StandardNames.HASHCODE_NAME && name != StandardNames.TO_STRING_NAME) {
                return
            }
        } else if (classId?.isAtomicType() == true) {
            if (name != StandardNames.TO_STRING_NAME) return
            if (expression.argumentList.arguments.isNotEmpty()) return
        }

        reporter.reportOn(expression.source, AtomicfuErrors.ATOMIC_DOES_NOT_INHERIT_FUNCTIONS_FROM_ANY)
    }

}
