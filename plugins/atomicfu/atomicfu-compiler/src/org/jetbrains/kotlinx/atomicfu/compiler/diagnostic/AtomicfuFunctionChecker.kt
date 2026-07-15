/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.text

object AtomicfuFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (declaration is FirPropertyAccessor) {
            // atomics are supposed to be properties, so there's nothing wrong in having a setter
            return
        }
        val receiverClassId = declaration.receiverParameter?.typeRef?.coneType?.classId
        if (receiverClassId?.isAtomicType() == true) {
            val visibility = declaration.publishedApiEffectiveVisibility ?: declaration.effectiveVisibility
            if (!declaration.isInline || visibility.publicApi) {
                reporter.reportOn(
                    declaration.source,
                    AtomicfuErrors.ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE,
                    declaration.source.text.toString()
                )
            }
        }
        declaration.valueParameters.forEach { parameter ->
            if (parameter.returnTypeRef.coneType.classId?.isAtomicType() == true) {
                reporter.reportOn(
                    parameter.source,
                    AtomicfuErrors.ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN,
                    parameter.source.text.toString()
                )
            }
        }
    }
}
