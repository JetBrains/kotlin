/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert.diagram

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.powerassert.irString

fun IrBuilderWithScope.irDiagramString(
    sourceFile: SourceFile,
    prefix: IrExpression? = null,
    call: IrCall,
    variables: List<IrTemporaryVariable>,
): IrExpression {
    val callInfo = sourceFile.getSourceRangeInfo(call)
    val callIndent = callInfo.startColumnNumber

    val stackValues = variables.map { it.toValueDisplay(callIndent, callInfo) }

    val valuesByRow = stackValues.groupBy { it.row }
    val rows = sourceFile.getText(callInfo)
        .replace("\n" + " ".repeat(callIndent), "\n") // Remove additional indentation
        .split("\n")

    return irConcat().apply {
        if (prefix != null) addArgument(prefix)

        for ((row, rowSource) in rows.withIndex()) {
            val rowValues = valuesByRow[row]?.let { values -> values.sortedBy { it.indent } } ?: emptyList()
            val indentations = rowValues.map { it.indent }

            addArgument(
                irString {
                    appendLine()
                    append(rowSource)
                    if (indentations.isNotEmpty()) {
                        appendLine()
                        var last = -1
                        for (i in indentations) {
                            if (i > last) indent(i - last - 1).append("|")
                            last = i
                        }
                    }
                },
            )

            for (tmp in rowValues.asReversed()) {
                addArgument(
                    irString {
                        appendLine()
                        var last = -1
                        for (i in indentations) {
                            if (i == tmp.indent) break
                            if (i > last) indent(i - last - 1).append("|")
                            last = i
                        }
                        indent(tmp.indent - last - 1)
                    },
                )
                addArgument(irGet(tmp.value))
            }
        }

        addArgument(
            irString {
                appendLine()
            }
        )
    }
}

private data class ValueDisplay(
    val value: IrVariable,
    val indent: Int,
    val row: Int,
    val source: String,
)

private fun IrTemporaryVariable.toValueDisplay(
    callIndent: Int,
    originalInfo: SourceRangeInfo,
): ValueDisplay {
    var indent = sourceRangeInfo.startColumnNumber - callIndent
    var row = sourceRangeInfo.startLineNumber - originalInfo.startLineNumber

    val source = text.replace("\n" + " ".repeat(callIndent), "\n") // Remove additional indentation
    val columnOffset = findDisplayOffset(original, sourceRangeInfo, source)

    val prefix = source.substring(0, columnOffset)
    val rowShift = prefix.count { it == '\n' }
    if (rowShift == 0) {
        indent += columnOffset
    } else {
        row += rowShift
        indent = columnOffset - (prefix.lastIndexOf('\n') + 1)
    }

    return ValueDisplay(temporary, indent, row, source)
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
private fun findDisplayOffset(
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
    val owner = expression.symbol.owner
    if (owner !is IrSimpleFunction) return 0

    if (owner.isInfix || owner.isOperator || owner.origin == IrBuiltIns.BUILTIN_OPERATOR) {
        val lhs = expression.binaryOperatorLhs() ?: return 0
        return binaryOperatorOffset(lhs, sourceRangeInfo, source)
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
private fun IrMemberAccessExpression<*>.binaryOperatorLhs(): IrExpression? = when (origin) {
    IrStatementOrigin.EXCLEQ -> {
        // The `!=` operator call is actually a sugar for `lhs.equals(rhs).not()`.
        (dispatchReceiver as? IrCall)?.simpleBinaryOperatorLhs()
    }
    IrStatementOrigin.EXCLEQEQ -> {
        // The `!==` operator call is actually a sugar for `(lhs === rhs).not()`.
        (dispatchReceiver as? IrCall)?.simpleBinaryOperatorLhs()
    }
    IrStatementOrigin.IN -> {
        // The `in` operator call is actually a sugar for `rhs.contains(lhs)`.
        getValueArgument(0)
    }
    IrStatementOrigin.NOT_IN -> {
        // The `!in` operator call is actually a sugar for `rhs.contains(lhs).not()`.
        (dispatchReceiver as? IrCall)?.getValueArgument(0)
    }
    else -> simpleBinaryOperatorLhs()
}

/**
 * The left-hand side expression of an infix operator/function.
 * For single-value operators returns `null`, for all other infix operators/functions, returns the receiver or the first value argument.
 */
private fun IrMemberAccessExpression<*>.simpleBinaryOperatorLhs(): IrExpression? {
    val singleReceiver = (dispatchReceiver != null) xor (extensionReceiver != null)
    return if (singleReceiver && valueArgumentsCount == 0) {
        null
    } else {
        dispatchReceiver
            ?: extensionReceiver
            ?: getValueArgument(0).takeIf { (symbol.owner as? IrSimpleFunction)?.origin == IrBuiltIns.BUILTIN_OPERATOR }
    }
}

fun StringBuilder.indent(indentation: Int): StringBuilder {
    repeat(indentation) { append(" ") }
    return this
}
