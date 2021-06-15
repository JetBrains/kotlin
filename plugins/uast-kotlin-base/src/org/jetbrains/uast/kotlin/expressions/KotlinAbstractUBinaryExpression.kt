/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UastBinaryOperator

abstract class KotlinAbstractUBinaryExpression(
    override val sourcePsi: KtBinaryExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UBinaryExpression, KotlinUElementWithType, KotlinEvaluatableUElement {

    protected companion object {
        val BITWISE_OPERATORS = mapOf(
            "or" to UastBinaryOperator.BITWISE_OR,
            "and" to UastBinaryOperator.BITWISE_AND,
            "xor" to UastBinaryOperator.BITWISE_XOR,
            "shl" to UastBinaryOperator.SHIFT_LEFT,
            "shr" to UastBinaryOperator.SHIFT_RIGHT,
            "ushr" to UastBinaryOperator.UNSIGNED_SHIFT_RIGHT
        )
    }

    override val leftOperand by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.left, this)
    }

    override val rightOperand by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.right, this)
    }

    override val operatorIdentifier: UIdentifier by lz {
        KotlinUIdentifier(sourcePsi.operationReference.getReferencedNameElement(), this)
    }

    override fun resolveOperator(): PsiMethod? =
        baseResolveProviderService.resolveCall(sourcePsi)

    override val operator: UastBinaryOperator
        get() = when (sourcePsi.operationToken) {
            KtTokens.EQ -> UastBinaryOperator.ASSIGN
            KtTokens.PLUS -> UastBinaryOperator.PLUS
            KtTokens.MINUS -> UastBinaryOperator.MINUS
            KtTokens.MUL -> UastBinaryOperator.MULTIPLY
            KtTokens.DIV -> UastBinaryOperator.DIV
            KtTokens.PERC -> UastBinaryOperator.MOD
            KtTokens.OROR -> UastBinaryOperator.LOGICAL_OR
            KtTokens.ANDAND -> UastBinaryOperator.LOGICAL_AND
            KtTokens.EQEQ -> UastBinaryOperator.EQUALS
            KtTokens.EXCLEQ -> UastBinaryOperator.NOT_EQUALS
            KtTokens.EQEQEQ -> UastBinaryOperator.IDENTITY_EQUALS
            KtTokens.EXCLEQEQEQ -> UastBinaryOperator.IDENTITY_NOT_EQUALS
            KtTokens.GT -> UastBinaryOperator.GREATER
            KtTokens.GTEQ -> UastBinaryOperator.GREATER_OR_EQUALS
            KtTokens.LT -> UastBinaryOperator.LESS
            KtTokens.LTEQ -> UastBinaryOperator.LESS_OR_EQUALS
            KtTokens.PLUSEQ -> UastBinaryOperator.PLUS_ASSIGN
            KtTokens.MINUSEQ -> UastBinaryOperator.MINUS_ASSIGN
            KtTokens.MULTEQ -> UastBinaryOperator.MULTIPLY_ASSIGN
            KtTokens.DIVEQ -> UastBinaryOperator.DIVIDE_ASSIGN
            KtTokens.PERCEQ -> UastBinaryOperator.REMAINDER_ASSIGN
            KtTokens.IN_KEYWORD -> KotlinBinaryOperators.IN
            KtTokens.NOT_IN -> KotlinBinaryOperators.NOT_IN
            KtTokens.RANGE -> KotlinBinaryOperators.RANGE_TO
            else -> handleBitwiseOperators()
        }

    abstract fun handleBitwiseOperators(): UastBinaryOperator
}
