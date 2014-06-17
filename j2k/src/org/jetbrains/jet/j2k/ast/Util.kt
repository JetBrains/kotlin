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

import java.util.ArrayList
import org.jetbrains.jet.j2k.CommentConverter

fun List<Element>.toKotlin(commentConverter: CommentConverter, separator: String, prefix: String = "", postfix: String = ""): String
        = if (isNotEmpty()) map { it.toKotlin(commentConverter) }.makeString(separator, prefix, postfix) else ""

fun String.withSuffix(suffix: String): String = if (isEmpty()) "" else this + suffix
fun String.withPrefix(prefix: String): String = if (isEmpty()) "" else prefix + this
fun Expression.withPrefix(prefix: String, commentConverter: CommentConverter): String = if (isEmpty) "" else prefix + toKotlin(commentConverter)

open class WhiteSpaceSeparatedElementList(
        val elements: List<Element>,
        val minimalWhiteSpace: WhiteSpace,
        val ensureSurroundedByWhiteSpace: Boolean = true
) {
    val nonEmptyElements = elements.filter { !it.isEmpty }

    fun isEmpty() = nonEmptyElements.all { it is WhiteSpace }

    fun toKotlin(commentConverter: CommentConverter): String {
        if (isEmpty()) return ""
        return nonEmptyElements.surroundWithWhiteSpaces().insertAndMergeWhiteSpaces().map { it.toKotlin(commentConverter) }.makeString("")
    }

    private fun List<Element>.surroundWithWhiteSpaces(): List<Element>
            = if (ensureSurroundedByWhiteSpace) listOf(minimalWhiteSpace) + this + listOf(minimalWhiteSpace) else this


    // ensure that there is whitespace between non-whitespace elements
    // choose maximum among subsequent whitespaces
    // all resulting whitespaces are at least minimal whitespace
    private fun List<Element>.insertAndMergeWhiteSpaces(): List<Element> {
        var currentWhiteSpace: WhiteSpace? = null
        val result = ArrayList<Element>()
        for (i in 0..lastIndex) {
            val element = get(i)
            if (element is WhiteSpace) {
                if (currentWhiteSpace == null || element > currentWhiteSpace!!) {
                    currentWhiteSpace = if (element > minimalWhiteSpace) element else minimalWhiteSpace
                }
            }
            else {
                if (i != 0) { //do not insert whitespace before first element
                    result.add(currentWhiteSpace ?: minimalWhiteSpace)
                }
                result.add(element)
                currentWhiteSpace = null
            }
        }
        if (currentWhiteSpace != null) {
            result.add(currentWhiteSpace!!)
        }
        return result
    }
}

fun Expression.operandToKotlin(operand: Expression, commentConverter: CommentConverter, parenthesisForSamePrecedence: Boolean = false): String {
    val parentPrecedence = precedence() ?: throw IllegalArgumentException("Unknown precendence for $this")
    val kotlinCode = operand.toKotlin(commentConverter)
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