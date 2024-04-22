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

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression

object ComposablePropertyAccessExpressionChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirPropertyAccessExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()
            ?: return
        if (calleeFunction.isComposable(context.session)) {
            checkComposableCall(expression, calleeFunction, context, reporter)
        }
    }
}

object ComposableFunctionCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()
            ?: return

        // K2 propagates annotation from the fun interface method to the constructor.
        // https://youtrack.jetbrains.com/issue/KT-47708.
        if (calleeFunction.origin == FirDeclarationOrigin.SamConstructor) return

        if (calleeFunction.isComposable(context.session)) {
            checkComposableCall(expression, calleeFunction, context, reporter)
        } else if (calleeFunction.callableId.isInvoke()) {
            checkInvoke(expression, context, reporter)
        }
    }
}

/**
 * Check if `expression` - a call to a composable function or access to a composable property -
 * is allowed in the current context. It is allowed if:
 *
 * - It is executed as part of the body of a composable function.
 * - It is not executed as part of the body of a lambda annotated with `@DisallowComposableCalls`.
 * - It is not inside of a `try` block.
 * - It is a call to a readonly composable function if it is executed in the body of a function
 *   that is annotated with `@ReadOnlyComposable`.
 *
 * A function is composable if:
 * - It is annotated with `@Composable`.
 * - It is a lambda whose type is inferred to be `ComposableFunction`.
 * - It is an inline lambda whose enclosing function is composable.
 */
private fun checkComposableCall(
    expression: FirQualifiedAccessExpression,
    calleeFunction: FirCallableSymbol<*>,
    context: CheckerContext,
    reporter: DiagnosticReporter
) {
    context.visitCurrentScope(
        visitInlineLambdaParameter = { parameter ->
            if (parameter.returnTypeRef.hasDisallowComposableCallsAnnotation(context.session)) {
                reporter.reportOn(
                    expression.calleeReference.source,
                    ComposeErrors.CAPTURED_COMPOSABLE_INVOCATION,
                    parameter.symbol,
                    parameter.containingFunctionSymbol,
                    context
                )
            }
        },
        visitAnonymousFunction = { function ->
            if (function.typeRef.coneType.functionTypeKind(context.session) === ComposableFunction)
                return
            val functionPsi = function.psi
            if (functionPsi is KtFunctionLiteral || functionPsi is KtLambdaExpression ||
                functionPsi !is KtFunction
            ) {
                return@visitCurrentScope
            }
            val nonReadOnlyCalleeReference =
                if (!calleeFunction.isReadOnlyComposable(context.session)) {
                    expression.calleeReference.source
                } else {
                    null
                }
            if (checkComposableFunction(
                    function,
                    nonReadOnlyCalleeReference,
                    context,
                    reporter,
                ) == ComposableCheckForScopeStatus.STOP
            ) {
                return
            }
        },
        visitFunction = { function ->
            val nonReadOnlyCalleeReference =
                if (!calleeFunction.isReadOnlyComposable(context.session)) {
                    expression.calleeReference.source
                } else {
                    null
                }
            if (checkComposableFunction(
                    function,
                    nonReadOnlyCalleeReference,
                    context,
                    reporter,
                ) == ComposableCheckForScopeStatus.STOP
            ) {
                return
            }
        },
        visitTryExpression = { tryExpression, container ->
            // Only report an error if the composable call happens inside of the `try`
            // block. Composable calls are allowed inside of `catch` and `finally` blocks.
            if (container !is FirCatch && tryExpression.finallyBlock != container) {
                reporter.reportOn(
                    tryExpression.source,
                    ComposeErrors.ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE,
                    context
                )
            }
        },
    )
    reporter.reportOn(
        expression.calleeReference.source,
        ComposeErrors.COMPOSABLE_INVOCATION,
        context
    )
}

private enum class ComposableCheckForScopeStatus {
    STOP,
    CONTINUE,
}

/**
 * This function will be called by [visitCurrentScope], and this function determines
 * whether it will continue the composable element check for the scope or not
 * by returning [ComposableCheckForScopeStatus].
 */
private fun checkComposableFunction(
    function: FirFunction,
    nonReadOnlyCallInsideFunction: KtSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
): ComposableCheckForScopeStatus {
    // [function] is a function with "read-only" composable annotation, but it has a call
    // without "read-only" composable annotation.
    // -> report NONREADONLY_CALL_IN_READONLY_COMPOSABLE.
    if (function.hasComposableAnnotation(context.session)) {
        if (
            function.hasReadOnlyComposableAnnotation(context.session) &&
            nonReadOnlyCallInsideFunction != null
        ) {
            reporter.reportOn(
                nonReadOnlyCallInsideFunction,
                ComposeErrors.NONREADONLY_CALL_IN_READONLY_COMPOSABLE,
                context
            )
        }
        return ComposableCheckForScopeStatus.STOP
    }
    // We allow composable calls in local delegated properties.
    // The only call this could be is a getValue/setValue in the synthesized getter/setter.
    if (function is FirPropertyAccessor && function.propertySymbol.hasDelegate) {
        if (function.propertySymbol.isVar) {
            reporter.reportOn(
                function.source,
                ComposeErrors.COMPOSE_INVALID_DELEGATE,
                context
            )
        }
        // Only local variables can be implicitly composable, for top-level or class-level
        // declarations we require an explicit annotation.
        if (!function.propertySymbol.isLocal) {
            reporter.reportOn(
                function.propertySymbol.source,
                ComposeErrors.COMPOSABLE_EXPECTED,
                context
            )
        }
        return ComposableCheckForScopeStatus.STOP
    }
    // We've found a non-composable function which contains a composable call.
    val source = if (function is FirPropertyAccessor) {
        function.propertySymbol.source
    } else {
        function.source
    }
    reporter.reportOn(source, ComposeErrors.COMPOSABLE_EXPECTED, context)
    return ComposableCheckForScopeStatus.CONTINUE
}

/**
 * Reports an error if we are invoking a lambda parameter of an inline function in a context
 * where composable calls are not allowed, unless the lambda parameter is itself annotated
 * with `@DisallowComposableCalls`.
 */
private fun checkInvoke(
    expression: FirQualifiedAccessExpression,
    context: CheckerContext,
    reporter: DiagnosticReporter
) {
    // Check that we're invoking a value parameter of an inline function
    val param = (expression.dispatchReceiver as? FirPropertyAccessExpression)
        ?.calleeReference
        ?.toResolvedValueParameterSymbol()
        ?: return
    if (param.resolvedReturnTypeRef.hasDisallowComposableCallsAnnotation(context.session) ||
        !param.containingFunctionSymbol.isInline) {
        return
    }

    context.visitCurrentScope(
        visitInlineLambdaParameter = { parameter ->
            if (parameter.returnTypeRef.hasDisallowComposableCallsAnnotation(context.session)) {
                reporter.reportOn(
                    param.source,
                    ComposeErrors.MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION,
                    param,
                    parameter.symbol,
                    parameter.containingFunctionSymbol,
                    context
                )
            }
        }
    )
}

/**
 * Visits all (Anonymous)Functions and `try` expressions in the current scope until it finds
 * a declaration that introduces a new scope. Elements are visited from innermost to outermost.
 */
private inline fun CheckerContext.visitCurrentScope(
    visitInlineLambdaParameter: (FirValueParameter) -> Unit,
    visitAnonymousFunction: (FirAnonymousFunction) -> Unit = {},
    visitFunction: (FirFunction) -> Unit = {},
    visitTryExpression: (FirTryExpression, FirElement) -> Unit = { _, _ -> }
) {
    for ((elementIndex, element) in containingElements.withIndex().reversed()) {
        when (element) {
            is FirAnonymousFunction -> {
                if (element.inlineStatus == InlineStatus.Inline) {
                    findValueParameterForLambdaAtIndex(elementIndex)?.let { parameter ->
                        visitInlineLambdaParameter(parameter)
                    }
                }
                visitAnonymousFunction(element)
                if (element.inlineStatus != InlineStatus.Inline) {
                    return
                }
            }
            is FirFunction -> {
                visitFunction(element)
                return
            }
            is FirTryExpression -> {
                val container = containingElements.getOrNull(elementIndex + 1)
                    ?: continue
                visitTryExpression(element, container)
            }
            is FirProperty -> {
                // Coming from an initializer or delegate expression, otherwise we'd
                // have hit a FirFunction and would already report an error.
            }
            is FirValueParameter -> {
                // We're coming from a default value in a function declaration, we need to
                // look at the enclosing function.
            }
            is FirAnonymousObject, is FirAnonymousInitializer -> {
                // Anonymous objects don't change the current scope, continue.
            }
            is FirField -> {
                if (element.origin == FirDeclarationOrigin.Synthetic.DelegateField) {
                    // Delegating in constructor creates a synthetic field in FIR.
                    // Continue through in this case in case it is an anonymous declaration.
                } else {
                    // Other fields introduce new scope which cannot be composable.
                    return
                }
            }
            // Every other declaration introduces a new scope which cannot be composable.
            is FirDeclaration -> return
        }
    }
}

private fun CheckerContext.findValueParameterForLambdaAtIndex(
    elementIndex: Int
): FirValueParameter? {
    val function = containingElements.getOrNull(elementIndex) as? FirAnonymousFunction ?: return null
    val argumentList = containingElements.getOrNull(elementIndex - 1) as? FirResolvedArgumentList ?: return null
    val argument = argumentList.arguments.find { it is FirAnonymousFunctionExpression && it.anonymousFunction == function } ?: return null
    return argumentList.mapping[argument]
}
