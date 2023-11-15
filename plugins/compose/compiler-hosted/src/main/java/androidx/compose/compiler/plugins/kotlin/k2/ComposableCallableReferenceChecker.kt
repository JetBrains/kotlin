/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.k2

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.fir.types.resolvedType

/**
 * Report an error on composable function references.
 *
 * `FirFunctionTypeKindExtension` has very limited support for custom function references and
 * basically requires implementations to distinguish between reflective and non-reflective
 * function types. Since there are no reflective composable function types we cannot support
 * composable function references yet.
 */
object ComposableCallableReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirCallableReferenceAccess,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // The type of a function reference depends on the context where it is used.
        // We could allow non-reflective composable function references, but this would be fragile
        // and depend on details of the frontend resolution.
        val kind = expression.resolvedType.functionTypeKind(context.session)
        if (kind == ComposableFunction || kind == KComposableFunction) {
            reporter.reportOn(
                expression.source,
                ComposeErrors.COMPOSABLE_FUNCTION_REFERENCE,
                context
            )
        }
    }
}
