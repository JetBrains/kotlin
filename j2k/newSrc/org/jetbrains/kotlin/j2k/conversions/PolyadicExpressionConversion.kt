/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKExpression
import org.jetbrains.kotlin.j2k.tree.JKJavaPolyadicExpression
import org.jetbrains.kotlin.j2k.tree.JKOperator
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKBinaryExpressionImpl


class PolyadicExpressionConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return (if (element is JKJavaPolyadicExpression)
            convertPolyadic(element.operands.also { element.operands = emptyList() }, element.tokens)
        else
            element).also {
            recurse(it)
        }
    }

    private fun convertPolyadic(operands: List<JKExpression>, operators: List<JKOperator>): JKExpression {
        return if (operators.isEmpty())
            operands.first()
        else {
            val op = operators.reduce { a, b -> if (a.precedence > b.precedence) a else b }
            val index = operators.indexOf(op)
            val left = convertPolyadic(operands.subList(0, index + 1), operators.subList(0, index))
            val right = convertPolyadic(operands.subList(index + 1, operands.size), operators.subList(index + 1, operators.size))
            JKBinaryExpressionImpl(left, right, op)
        }
    }
}