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
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.withMargin

class KotlinULambdaExpression(
        override val psi: KtLambdaExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), ULambdaExpression, KotlinUElementWithType {
    override val functionalInterfaceType: PsiType?
        get() = getFunctionalInterfaceType()

    override val body by lz { KotlinConverter.convertOrEmpty(psi.bodyExpression, this) }
    
    override val valueParameters by lz {
        psi.valueParameters.mapIndexed { i, p ->
            KotlinUParameter(UastKotlinPsiParameter.create(p, psi, this, i), psi, this)
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