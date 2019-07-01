/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.parenthesizeIfBinaryExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKBinaryExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKParenthesizedExpressionImpl


class PolyadicExpressionConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaPolyadicExpression) return recurse(element)
        val needParenthesis = element.operands.any { it.containsNewLine() }
        val parenthesisedOperands = element::operands.detached().map { it.parenthesizeIfBinaryExpression() }
        val polyadic = convertPolyadic(parenthesisedOperands, element.tokens)

        return recurse(
            if (needParenthesis) JKParenthesizedExpressionImpl(polyadic)
            else polyadic
        )
    }

    private fun convertPolyadic(operands: List<JKExpression>, operators: List<JKOperator>): JKExpression {
        return if (operators.isEmpty())
            operands.first()
        else {
            val operator = operators.maxBy { it.precedence }!!
            val index = operators.indexOf(operator)
            val left = convertPolyadic(operands.subList(0, index + 1), operators.subList(0, index))
            val right = convertPolyadic(operands.subList(index + 1, operands.size), operators.subList(index + 1, operators.size))
            JKBinaryExpressionImpl(left, right, operator)
        }
    }
}