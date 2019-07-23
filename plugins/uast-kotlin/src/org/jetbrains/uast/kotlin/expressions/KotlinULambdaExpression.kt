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

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsResultOfLambda
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter

class KotlinULambdaExpression(
        override val sourcePsi: KtLambdaExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), ULambdaExpression, KotlinUElementWithType {
    override val functionalInterfaceType: PsiType?
        get() = getFunctionalInterfaceType()

    override val body by lz {
        sourcePsi.bodyExpression?.let { Body(it, this) } ?: UastEmptyExpression(this)
    }

    class Body(bodyExpression: KtBlockExpression, parent: KotlinULambdaExpression) : KotlinUBlockExpression(bodyExpression, parent) {

        override val expressions: List<UExpression> by lz expressions@{
            val statements = sourcePsi.statements
            if (statements.isEmpty()) return@expressions emptyList<UExpression>()
            ArrayList<UExpression>(statements.size).also { result ->
                statements.subList(0, statements.size - 1).mapTo(result) { KotlinConverter.convertOrEmpty(it, this) }
                result.add(implicitReturn ?: KotlinConverter.convertOrEmpty(statements.last(), this))
            }
        }

        val implicitReturn: KotlinUImplicitReturnExpression? by lz {
            val lastExpression = sourcePsi.statements.lastOrNull() ?: return@lz null
            if (!lastExpression.isUsedAsResultOfLambda(lastExpression.analyze())) return@lz null

            KotlinUImplicitReturnExpression(this).apply {
                returnExpression = KotlinConverter.convertOrEmpty(lastExpression, this)
            }
        }

    }

    override val valueParameters by lz {
        sourcePsi.valueParameters.mapIndexed { i, p ->
            KotlinUParameter(UastKotlinPsiParameter.create(p, sourcePsi, this, i), sourcePsi, this)
        }
    }
    
    override fun asRenderString(): String {
        val renderedValueParameters = if (valueParameters.isEmpty())
            ""
        else
            valueParameters.joinToString { it.asRenderString() } + " ->\n"
        val expressions = (body as? UBlockExpression)?.expressions
                                  ?.joinToString("\n") { it.asRenderString().withMargin } ?: body.asRenderString()

        return "{ $renderedValueParameters\n$expressions\n}"
    }
}