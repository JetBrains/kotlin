/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.lexer.KtTokens


class OperatorExpressionConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKOperatorExpression) return recurse(element)
        val operatorToken =
            (element.operator as? JKJavaOperatorImpl)?.token?.toKtToken() ?: return recurse(element)

        return when (element) {
            is JKBinaryExpression -> {
                val left = applyToElement(element::left.detached()) as JKExpression
                val right = applyToElement(element::right.detached()) as JKExpression
                recurse(convertBinaryExpression(left, right, operatorToken))
            }
            is JKPrefixExpression -> {
                val operand = applyToElement(element::expression.detached()) as JKExpression
                recurse(kotlinPrefixExpression(operand, operatorToken, context.symbolProvider))
            }
            is JKPostfixExpression -> {
                val operand = applyToElement(element::expression.detached()) as JKExpression
                recurse(kotlinPostfixExpression(operand, operatorToken, context.symbolProvider))
            }
            else -> TODO(element.javaClass.toString())
        } ?: recurse(element)
    }

    private fun convertBinaryExpression(left: JKExpression, right: JKExpression, token: JKKtOperatorToken): JKBinaryExpression =
        convertStringImplicitConcatenation(left, right, token)
            ?: kotlinBinaryExpression(left, right, token, context.symbolProvider)


    private fun convertStringImplicitConcatenation(left: JKExpression, right: JKExpression, token: JKKtOperatorToken): JKBinaryExpression? =
        if (token is JKKtSingleValueOperatorToken
            && token.psiToken == KtTokens.PLUS
            && right.type(context.symbolProvider)?.isStringType() == true
            && left.type(context.symbolProvider)?.isStringType() == false
        ) {
            val toStringCall =
                JKKtCallExpressionImpl(
                    context.symbolProvider.provideByFqName("kotlin.Any.toString"),
                    JKExpressionListImpl()
                )
            val qualifiedCall = JKQualifiedExpressionImpl(left, JKKtQualifierImpl.DOT, toStringCall)
            kotlinBinaryExpression(qualifiedCall, right, KtTokens.PLUS, context.symbolProvider)
        } else null
}