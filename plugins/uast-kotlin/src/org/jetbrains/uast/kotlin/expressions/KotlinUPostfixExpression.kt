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

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier
import org.jetbrains.uast.kotlin.internal.DelegatedMultiResolve

class KotlinUPostfixExpression(
        override val sourcePsi: KtPostfixExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UPostfixExpression, KotlinUElementWithType, KotlinEvaluatableUElement,
    UResolvable, DelegatedMultiResolve {
    override val operand by lz { KotlinConverter.convertOrEmpty(sourcePsi.baseExpression, this) }

    override val operator = when (sourcePsi.operationToken) {
        KtTokens.PLUSPLUS -> UastPostfixOperator.INC
        KtTokens.MINUSMINUS -> UastPostfixOperator.DEC
        KtTokens.EXCLEXCL -> KotlinPostfixOperators.EXCLEXCL
        else -> UastPostfixOperator.UNKNOWN
    }

    override val operatorIdentifier: UIdentifier?
        get() = KotlinUIdentifier(sourcePsi.operationReference, this)

    override fun resolveOperator(): PsiMethod? = resolveToPsiMethod(sourcePsi)

    override fun resolve(): PsiMethod? = when (sourcePsi.operationToken) {
        KtTokens.EXCLEXCL -> operand.tryResolve() as? PsiMethod
        else -> null
    }
}