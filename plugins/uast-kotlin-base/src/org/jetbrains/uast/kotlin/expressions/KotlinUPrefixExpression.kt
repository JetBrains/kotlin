/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.UastPrefixOperator

class KotlinUPrefixExpression(
    override val sourcePsi: KtPrefixExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UPrefixExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val operand by lz {
        baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.baseExpression, this)
    }

    override val operatorIdentifier: UIdentifier
        get() = KotlinUIdentifier(sourcePsi.operationReference, this)

    override fun resolveOperator(): PsiMethod? =
        baseResolveProviderService.resolveCall(sourcePsi)

    override val operator = when (sourcePsi.operationToken) {
        KtTokens.EXCL -> UastPrefixOperator.LOGICAL_NOT
        KtTokens.PLUS -> UastPrefixOperator.UNARY_PLUS
        KtTokens.MINUS -> UastPrefixOperator.UNARY_MINUS
        KtTokens.PLUSPLUS -> UastPrefixOperator.INC
        KtTokens.MINUSMINUS -> UastPrefixOperator.DEC
        else -> UastPrefixOperator.UNKNOWN
    }
}
