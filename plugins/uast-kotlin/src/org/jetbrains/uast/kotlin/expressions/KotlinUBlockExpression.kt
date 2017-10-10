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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.uast.*

class KotlinUBlockExpression(
        override val psi: KtBlockExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UBlockExpression, KotlinUElementWithType {
    override val expressions by lz { psi.statements.map { KotlinConverter.convertOrEmpty(it, this) } }

    private class KotlinLazyUBlockExpression(
            override val uastParent: UElement?,
            expressionProducer: (expressionParent: UElement?) -> List<UExpression>
    ) : UBlockExpression, JvmDeclarationUElement {
        override val psi: PsiElement? = null
        override val javaPsi: PsiElement? = null
        override val sourcePsi: PsiElement? = null
        override val annotations: List<UAnnotation> = emptyList()
        override val expressions by lz { expressionProducer(this) }
    }

    companion object {
        fun create(initializers: List<KtAnonymousInitializer>, uastParent: UElement): UBlockExpression {
            val languagePlugin = uastParent.getLanguagePlugin()
            return KotlinLazyUBlockExpression(uastParent) { expressionParent ->
                initializers.map { languagePlugin.convertOpt<UExpression>(it.body, expressionParent) ?: UastEmptyExpression }
            }
        }
    }
}