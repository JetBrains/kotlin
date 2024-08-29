/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.explanation

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.powerassert.diagram.IrTemporaryVariable
import org.jetbrains.kotlin.powerassert.getExplicitReceiver
import org.jetbrains.kotlin.powerassert.isInnerOfComparisonOperator
import org.jetbrains.kotlin.powerassert.isInnerOfNotEqualOperator

fun IrTemporaryVariable.toDisplayOffset(): Int {
    return sourceRangeInfo.startOffset + findDisplayOffset(original, sourceRangeInfo, text)
}

/**
 * Responsible for determining the diagram display offset of the expression
 * beginning from the startOffset of the expression.
 *
 * Equality:
 * ```
 * number == 42
 * | <- startOffset
 *        | <- display offset: 7
 * ```
 *
 * Arithmetic:
 * ```
 * i + 2
 * | <- startOffset
 *   | <- display offset: 2
 * ```
 *
 * Infix:
 * ```
 * 1 shl 2
 * | <- startOffset
 *   | <- display offset: 2
 * ```
 *
 * Standard:
 * ```
 * 1.shl(2)
 *   | <- startOffset
 *   | <- display offset: 0
 * ```
 */
internal fun findDisplayOffset(
    expression: IrExpression,
    sourceRangeInfo: SourceRangeInfo,
    source: String,
): Int {
    return when (expression) {
        is IrMemberAccessExpression<*> -> memberAccessOffset(expression, sourceRangeInfo, source)
        is IrTypeOperatorCall -> typeOperatorOffset(expression, sourceRangeInfo, source)
        else -> 0
    }
}

private fun memberAccessOffset(
    expression: IrMemberAccessExpression<*>,
    sourceRangeInfo: SourceRangeInfo,
    source: String,
): Int {
    if (expression !is IrCall) return 0
    val owner = expression.symbol.owner
    if (owner.isInfix || owner.isOperator || owner.origin == IrBuiltIns.BUILTIN_OPERATOR) {
        val lhs = expression.binaryOperatorLhs() ?: return 0
        return when (expression.origin) {
            IrStatementOrigin.GET_ARRAY_ELEMENT -> lhs.endOffset - sourceRangeInfo.startOffset
            else -> binaryOperatorOffset(lhs, sourceRangeInfo, source)
        }
    }

    return 0
}

private fun typeOperatorOffset(
    expression: IrTypeOperatorCall,
    sourceRangeInfo: SourceRangeInfo,
    source: String,
): Int {
    return when (expression.operator) {
        IrTypeOperator.INSTANCEOF,
        IrTypeOperator.NOT_INSTANCEOF,
        IrTypeOperator.SAFE_CAST,
            -> binaryOperatorOffset(expression.argument, sourceRangeInfo, source)

        else -> 0
    }
}

/**
 * The offset of the infix operator/function token itself.
 *
 * @param lhs The left-hand side expression of the operator.
 * @param wholeOperatorSourceRangeInfo The source range of the whole operator expression.
 * @param wholeOperatorSource The source text of the whole operator expression.
 */
private fun binaryOperatorOffset(lhs: IrExpression, wholeOperatorSourceRangeInfo: SourceRangeInfo, wholeOperatorSource: String): Int {
    val offset = lhs.endOffset - wholeOperatorSourceRangeInfo.startOffset
    if (offset < 0 || offset >= wholeOperatorSource.length) return 0 // infix function called using non-infix syntax

    KotlinLexer().run {
        start(wholeOperatorSource, offset, wholeOperatorSource.length)
        while (tokenType != null && tokenType != KtTokens.EOF && (tokenType == KtTokens.DOT || tokenType !in KtTokens.OPERATIONS)) {
            advance()
        }
        if (tokenStart >= wholeOperatorSource.length) return 0
        return tokenStart
    }
}

/**
 * The left-hand side expression of an infix operator/function that takes into account special cases like `in`, `!in` and `!=` operators
 * that have a more complex structure than just a single call with two arguments.
 */
private fun IrCall.binaryOperatorLhs(): IrExpression? = when (origin) {
    IrStatementOrigin.EXCLEQ, IrStatementOrigin.EXCLEQEQ -> {
        // The `!=` operator call is actually a sugar for `lhs.equals(rhs).not()`.
        // The `!==` operator call is actually a sugar for `(lhs === rhs).not()`.
        val innerCall = (arguments[0] as? IrCall)?.takeIf { it.isInnerOfNotEqualOperator() } ?: this
        innerCall.simpleBinaryOperatorLhs()
    }
    IrStatementOrigin.IN -> {
        // The `in` operator call is actually a sugar for `rhs.contains(lhs)`.
        arguments.last()
    }
    IrStatementOrigin.NOT_IN -> {
        // The `!in` operator call is actually a sugar for `rhs.contains(lhs).not()`.
        (arguments[0] as? IrCall)?.arguments?.last()
    }
    IrStatementOrigin.LT, IrStatementOrigin.GT, IrStatementOrigin.LTEQ, IrStatementOrigin.GTEQ -> {
        // Comparison operator calls are actually sugar for `lhs.compareTo(rhs) <> 0`.
        val innerCall = (arguments[0] as? IrCall)?.takeIf { it.isInnerOfComparisonOperator() } ?: this
        innerCall.simpleBinaryOperatorLhs()
    }
    else -> simpleBinaryOperatorLhs()
}

/**
 * The left-hand side expression of an infix operator/function:
 * * For single-value operators returns `null`,
 * * For all others, returns the explicit receiver or the first regular argument.
 */
private fun IrCall.simpleBinaryOperatorLhs(): IrExpression? {
    val parameters = symbol.owner.parameters
    val singleReceiver =
        1 == parameters.count { it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver }
    if (singleReceiver && parameters.none { it.kind == IrParameterKind.Regular }) {
        return null
    }

    getExplicitReceiver()?.let { return it }
    if (symbol.owner.origin != IrBuiltIns.BUILTIN_OPERATOR) return null
    return parameters.firstOrNull { it.kind == IrParameterKind.Regular }?.let { arguments[it] }
}
