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

import org.jetbrains.jet.j2k.CommentsAndSpaces

fun List<Element>.toKotlin(commentsAndSpaces: CommentsAndSpaces, separator: String, prefix: String = "", postfix: String = ""): String {
    val texts = map { it.toKotlin(commentsAndSpaces) }.filter { it.isNotEmpty() }
    return if (texts.isNotEmpty()) texts.makeString(separator, prefix, postfix) else ""
}

fun String.withSuffix(suffix: String): String = if (isEmpty()) "" else this + suffix
fun String.withPrefix(prefix: String): String = if (isEmpty()) "" else prefix + this
fun Expression.withPrefix(prefix: String, commentsAndSpaces: CommentsAndSpaces): String = if (isEmpty) "" else prefix + toKotlin(commentsAndSpaces)

fun Expression.operandToKotlin(operand: Expression, commentsAndSpaces: CommentsAndSpaces, parenthesisForSamePrecedence: Boolean = false): String {
    val parentPrecedence = precedence() ?: throw IllegalArgumentException("Unknown precendence for $this")
    val kotlinCode = operand.toKotlin(commentsAndSpaces)
    val operandPrecedence = operand.precedence() ?: return kotlinCode
    val needParenthesis = parentPrecedence < operandPrecedence || parentPrecedence == operandPrecedence && parenthesisForSamePrecedence
    return if (needParenthesis) "($kotlinCode)" else kotlinCode
}

private fun Expression.precedence(): Int? {
    return when(this) {
        is QualifiedExpression, is MethodCallExpression, is ArrayAccessExpression, is PostfixOperator, is BangBangExpression -> 0

        is PrefixOperator -> 1

        is TypeCastExpression -> 2

        is BinaryExpression -> when(op) {
            "*", "/", "%" -> 3
            "+", "-" -> 4
            "?:" -> 6
            ">", "<", ">=", "<=" -> 8
            "==", "!=", "===", "!===" -> 9
            "&&" -> 10
            "||" -> 11
            else -> 5 /* simple name */
        }

        is IsOperator -> 7

        is IfStatement -> 12

        is AssignmentExpression -> 13

        else -> null
    }
}