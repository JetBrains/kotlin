/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUBinaryExpression(
        override val psi: KtBinaryExpression,
        override val parent: UElement
) : UBinaryExpression, PsiElementBacked, KotlinTypeHelper, KotlinEvaluateHelper {
    override val leftOperand by lz { KotlinConverter.convertOrEmpty(psi.left, this) }
    override val rightOperand by lz { KotlinConverter.convertOrEmpty(psi.right, this) }

    override val operator = when (psi.operationToken) {
        KtTokens.PLUS -> UastBinaryOperator.PLUS
        KtTokens.MINUS -> UastBinaryOperator.MINUS
        KtTokens.MUL -> UastBinaryOperator.MULT
        KtTokens.DIV -> UastBinaryOperator.DIV
        KtTokens.PERC -> UastBinaryOperator.MOD
        KtTokens.EQEQ -> UastBinaryOperator.EQUALS
        KtTokens.EXCLEQ -> UastBinaryOperator.NOT_EQUALS
        KtTokens.EQEQEQ -> UastBinaryOperator.IDENTITY_EQUALS
        KtTokens.EXCLEQEQEQ -> UastBinaryOperator.IDENTITY_NOT_EQUALS
        KtTokens.GT -> UastBinaryOperator.GREATER
        KtTokens.GTEQ -> UastBinaryOperator.GREATER_OR_EQUAL
        KtTokens.LT -> UastBinaryOperator.LESS
        KtTokens.LTEQ -> UastBinaryOperator.LESS_OR_EQUAL
        KtTokens.IN_KEYWORD -> KotlinBinaryOperators.IN
        KtTokens.NOT_IN -> KotlinBinaryOperators.NOT_IN
        else -> UastBinaryOperator.UNKNOWN
    }
}

class KotlinCustomUBinaryExpression(
        override val psi: PsiElement,
        override val parent: UElement
) : UBinaryExpression, PsiElementBacked, NoEvaluate {
    lateinit override var leftOperand: UExpression
        internal set

    lateinit override var operator: UastBinaryOperator
        internal set

    lateinit override var rightOperand: UExpression
        internal set
}