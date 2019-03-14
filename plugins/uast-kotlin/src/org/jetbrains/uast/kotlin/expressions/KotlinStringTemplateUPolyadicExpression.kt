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

import com.intellij.psi.PsiLanguageInjectionHost
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost

class KotlinStringTemplateUPolyadicExpression(
    override val psi: KtStringTemplateExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent),
    UPolyadicExpression,
    KotlinUElementWithType,
    KotlinEvaluatableUElement,
    UInjectionHost {
    override val operands: List<UExpression> by lz {
        psi.entries.map {
            KotlinConverter.convertEntry(
                it,
                this,
                DEFAULT_EXPRESSION_TYPES_LIST
            )!!
        }
    }
    override val operator = UastBinaryOperator.PLUS

    override val psiLanguageInjectionHost: PsiLanguageInjectionHost get() = psi
    override val isString: Boolean get() = true

    override fun asRenderString(): String = if (operands.isEmpty()) "\"\"" else super<UPolyadicExpression>.asRenderString()
    override fun asLogString(): String = if (operands.isEmpty()) "UPolyadicExpression (value = \"\")" else super.asLogString()
}