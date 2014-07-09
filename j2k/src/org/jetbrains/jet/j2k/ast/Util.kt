/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.*

fun String.withSuffix(suffix: String): String = if (isEmpty()) "" else this + suffix
fun String.withPrefix(prefix: String): String = if (isEmpty()) "" else prefix + this

fun CodeBuilder.appendWithPrefix(element: Element, prefix: String): CodeBuilder = if (!element.isEmpty) this append prefix append element else this
fun CodeBuilder.appendWithSuffix(element: Element, suffix: String): CodeBuilder = if (!element.isEmpty) this append element append suffix else this

fun CodeBuilder.appendOperand(expression: Expression, operand: Expression, parenthesisForSamePrecedence: Boolean = false): CodeBuilder {
    val parentPrecedence = expression.precedence() ?: throw IllegalArgumentException("Unknown precendence for $this")
    val operandPrecedence = operand.precedence()
    val needParenthesis = operandPrecedence != null &&
            (parentPrecedence < operandPrecedence || parentPrecedence == operandPrecedence && parenthesisForSamePrecedence)
    if (needParenthesis) append("(")
    append(operand)
    if (needParenthesis) append(")")
    return this
}

private fun Expression.precedence(): Int? {
    return when(this) {
        is QualifiedExpression, is MethodCallExpression, is ArrayAccessExpression, is PostfixOperator, is BangBangExpression, is StarExpression -> 0

        is PrefixOperator -> 1

        is TypeCastExpression -> 2

        is BinaryExpression -> when(op) {
            "*", "/", "%" -> 3
            "+", "-" -> 4
            "?:" -> 7
            ">", "<", ">=", "<=" -> 9
            "==", "!=", "===", "!===" -> 10
            "&&" -> 11
            "||" -> 12
            else -> 6 /* simple name */
        }

        is RangeExpression -> 5

        is IsOperator -> 8

        is IfStatement -> 13

        is AssignmentExpression -> 14

        else -> null
    }
}