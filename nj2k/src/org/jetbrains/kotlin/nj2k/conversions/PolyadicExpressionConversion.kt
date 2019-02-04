/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.kotlinBinaryExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*


class PolyadicExpressionConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return recurse(
            if (element is JKJavaPolyadicExpression)
                convertPolyadic(
                    element.operands.also { element.operands = emptyList() },
                    element.tokens
                )
            else element
        )
    }

    private fun convertPolyadic(operands: List<JKExpression>, operators: List<JKOperator>): JKExpression {
        return if (operators.isEmpty())
            operands.first()
        else {
            val operator = operators.maxBy { it.precedence }
            when (operator) {
                is JKJavaOperatorImpl -> operator.token.toKtToken()
                is JKKtOperatorImpl -> operator.token
                else -> error("operator should be either kotlin or java")
            }
            val index = operators.indexOf(operator)
            val left = convertPolyadic(operands.subList(0, index + 1), operators.subList(0, index))
            val right = convertPolyadic(operands.subList(index + 1, operands.size), operators.subList(index + 1, operators.size))
            JKBinaryExpressionImpl(left, right, operator)
        }
    }
}