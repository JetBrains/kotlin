/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkAtomicReferenceAccess
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicfuStandardClassIds

object AtomicfuAtomicRefToPrimitiveCallChecker : FirFunctionCallChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callable = expression.calleeReference.resolved?.resolvedSymbol as? FirFunctionSymbol<*>

        if (callable?.callableId == AtomicfuStandardClassIds.Callables.atomic) {
            checkAtomicReferenceAccess(
                expression.resolvedType, expression.source,
                AtomicfuStandardClassIds.AtomicRef, emptyMap(),
                context, reporter,
            )
        }
    }
}
