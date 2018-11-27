/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.kotlinBinaryExpression
import org.jetbrains.kotlin.j2k.kotlinPostfixExpression
import org.jetbrains.kotlin.j2k.kotlinPrefixExpression
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaOperatorImpl
import org.jetbrains.kotlin.j2k.tree.impl.toKtToken


class OperatorExpressionConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKOperatorExpression) return recurse(element)
        val operatorToken =
            (element.operator as? JKJavaOperatorImpl)?.token?.toKtToken() ?: return recurse(element)

        return when (element) {
            is JKBinaryExpression -> {
                val left = applyToElement(element::left.detached()) as JKExpression
                val right = applyToElement(element::right.detached()) as JKExpression
                kotlinBinaryExpression(left, right, operatorToken, context)?.let { recurse(it) }
            }
            is JKPrefixExpression -> {
                val operand = applyToElement(element::expression.detached()) as JKExpression
                kotlinPrefixExpression(operand, operatorToken, context)?.let { recurse(it) }
            }
            is JKPostfixExpression -> {
                val operand = applyToElement(element::expression.detached()) as JKExpression
                kotlinPostfixExpression(operand, operatorToken, context)?.let { recurse(it) }
            }
            else -> TODO(element.javaClass.toString())
        } ?: recurse(element)
    }
}