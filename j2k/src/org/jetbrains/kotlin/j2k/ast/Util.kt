/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.CodeBuilder

fun CodeBuilder.appendWithPrefix(element: Element, prefix: String): CodeBuilder = if (!element.isEmpty) this append prefix append element else this
fun CodeBuilder.appendWithSuffix(element: Element, suffix: String): CodeBuilder = if (!element.isEmpty) this append element append suffix else this

fun CodeBuilder.appendOperand(expression: Expression, operand: Expression, parenthesisForSamePrecedence: Boolean = false): CodeBuilder {
    val parentPrecedence = expression.precedence() ?: throw IllegalArgumentException("Unknown precendence for $expression")
    val operandPrecedence = operand.precedence()
    val needParenthesis = operandPrecedence != null &&
            (parentPrecedence < operandPrecedence || parentPrecedence == operandPrecedence && parenthesisForSamePrecedence)
    if (needParenthesis) append("(")
    append(operand)
    if (needParenthesis) append(")")
    return this
}

fun Element.wrapToBlockIfRequired(): Element = when (this) {
    is AssignmentExpression -> if (isMultiAssignment()) Block.of(this).assignNoPrototype() else this
    else -> this
}


private fun Expression.precedence(): Int? {
    return when (this) {
        is QualifiedExpression, is MethodCallExpression, is ArrayAccessExpression, is PostfixExpression, is BangBangExpression, is StarExpression -> 0

        is PrefixExpression -> 1

        is TypeCastExpression -> 2

        is BinaryExpression -> op.precedence

        is RangeExpression, is UntilExpression, is DownToExpression -> 5

        is IsOperator -> 8

        is IfStatement -> 13

        is AssignmentExpression -> 14

        else -> null
    }
}
