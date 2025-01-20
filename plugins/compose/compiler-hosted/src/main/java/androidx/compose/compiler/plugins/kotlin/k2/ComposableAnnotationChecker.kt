/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.k2

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationChecker
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

class ComposableAnnotationChecker : FirAnnotationChecker(MppCheckerKind.Common) {
    override fun check(expression: FirAnnotation, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.resolvedType.classId != ComposeClassIds.Composable) {
            return
        }

        val containingType = context.containingElements.lastIsInstanceOrNull<FirTypeRef>()
        if (
            containingType != null &&
            !containingType.coneType.isComposableFunction(context.session) &&
            // suspend functions are handled by checking function kinds, so no need to report additional diagnostics
            !containingType.coneType.isSuspendOrKSuspendFunctionType(context.session)
        ) {
            reporter.reportOn(
                expression.source,
                ComposeErrors.COMPOSABLE_INAPPLICABLE_TYPE,
                containingType.coneType,
                context
            )
        }
    }
}