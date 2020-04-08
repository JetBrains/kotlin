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

open class KotlinUBlockExpression(
    override val sourcePsi: KtBlockExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UBlockExpression, KotlinUElementWithType {
    override val expressions by lz { sourcePsi.statements.map { KotlinConverter.convertOrEmpty(it, this) } }

    class KotlinLazyUBlockExpression(
            override val uastParent: UElement?,
            expressionProducer: (expressionParent: UElement) -> List<UExpression>
    ) : UBlockExpression, JvmDeclarationUElementPlaceholder {
        override val psi: PsiElement? get() = null
        override val javaPsi: PsiElement? get() = null
        override val sourcePsi: PsiElement? get() = null
        override val annotations: List<UAnnotation> = emptyList()
        override val expressions by lz { expressionProducer(this) }
    }

    companion object {
        fun create(initializers: List<KtAnonymousInitializer>, uastParent: UElement): UBlockExpression {
            val languagePlugin = uastParent.getLanguagePlugin()
            return KotlinLazyUBlockExpression(uastParent) { expressionParent ->
                initializers.map { languagePlugin.convertOpt<UExpression>(it.body, expressionParent) ?: UastEmptyExpression(expressionParent) }
            }
        }
    }

    override fun convertParent(): UElement? {
        val directParent = super.convertParent()
        if (directParent is UnknownKotlinExpression && directParent.sourcePsi is KtAnonymousInitializer) {
            val containingUClass = directParent.getContainingUClass() ?: return directParent
            containingUClass.methods
                    .find { it is KotlinConstructorUMethod && it.isPrimary || it is KotlinSecondaryConstructorWithInitializersUMethod }?.let {
                return it.uastBody
            }
        }
        return directParent
    }
}