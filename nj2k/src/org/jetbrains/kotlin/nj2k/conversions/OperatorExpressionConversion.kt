/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.JavaTokenType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.kotlinBinaryExpression
import org.jetbrains.kotlin.nj2k.kotlinPostfixExpression
import org.jetbrains.kotlin.nj2k.kotlinPrefixExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*


class OperatorExpressionConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKOperatorExpression) return recurse(element)
        val operator = element.operator as? JKJavaOperatorImpl ?: return recurse(element)

        return when (element) {
            is JKBinaryExpression -> {
                val operatorToken = operator.token.toKtToken()
                val left = applyToElement(element::left.detached()) as JKExpression
                val right = applyToElement(element::right.detached()) as JKExpression
                recurse(convertBinaryExpression(left, right, operatorToken).withNonCodeElementsFrom(element))
            }
            is JKPrefixExpression -> {
                val operand = applyToElement(element::expression.detached()) as JKExpression
                recurse(convertPrefixExpression(operand, operator))
            }
            is JKPostfixExpression -> {
                val operatorToken = operator.token.toKtToken()
                val operand = applyToElement(element::expression.detached()) as JKExpression
                recurse(kotlinPostfixExpression(operand, operatorToken, typeFactory))
            }
            else -> TODO(element.javaClass.toString())
        }.withNonCodeElementsFrom(element)
    }


    private fun convertPrefixExpression(operand: JKExpression, javaOperator: JKJavaOperatorImpl) =
        convertTildeExpression(operand, javaOperator)
            ?: kotlinPrefixExpression(operand, javaOperator.token.toKtToken(), typeFactory)

    private fun convertTildeExpression(operand: JKExpression, javaOperator: JKJavaOperatorImpl): JKExpression? =
        if (javaOperator.token.psiToken == JavaTokenType.TILDE) {
            val invCall =
                JKKtCallExpressionImpl(
                    symbolProvider.provideMethodSymbol("kotlin.Int.inv"),//TODO check if Long
                    JKArgumentListImpl()
                )
            JKQualifiedExpressionImpl(
                JKParenthesizedExpressionImpl(operand),
                JKKtQualifierImpl.DOT,
                invCall
            )
        } else null

    private fun convertBinaryExpression(left: JKExpression, right: JKExpression, token: JKKtOperatorToken): JKBinaryExpression =
        convertStringImplicitConcatenation(left, right, token)
            ?: kotlinBinaryExpression(left, right, token, typeFactory)


    private fun convertStringImplicitConcatenation(left: JKExpression, right: JKExpression, token: JKKtOperatorToken): JKBinaryExpression? =
        if (token is JKKtSingleValueOperatorToken
            && token.psiToken == KtTokens.PLUS
            && right.type(typeFactory)?.isStringType() == true
            && left.type(typeFactory)?.isStringType() == false
        ) {
            val toStringCall =
                JKKtCallExpressionImpl(
                    symbolProvider.provideMethodSymbol("kotlin.Any.toString"),
                    JKArgumentListImpl()
                )
            val qualifiedCall = JKQualifiedExpressionImpl(left, JKKtQualifierImpl.DOT, toStringCall)
            kotlinBinaryExpression(qualifiedCall, right, KtTokens.PLUS, typeFactory)
        } else null
}