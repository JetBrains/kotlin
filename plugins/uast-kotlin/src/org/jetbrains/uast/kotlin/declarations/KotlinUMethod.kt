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

package org.jetbrains.uast.kotlin.declarations

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isGetter
import org.jetbrains.kotlin.asJava.elements.isSetter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.java.annotations
import org.jetbrains.uast.java.internal.JavaUElementWithComments
import org.jetbrains.uast.kotlin.*

open class KotlinUMethod(
        psi: KtLightMethod,
        override val uastParent: UElement?
) : UAnnotationMethod, JavaUElementWithComments, PsiMethod by psi {
    override val psi: KtLightMethod = unwrap<UMethod, KtLightMethod>(psi)

    override val uastDefaultValue by lz {
        val annotationParameter = psi.kotlinOrigin as? KtParameter ?: return@lz null
        val defaultValue = annotationParameter.defaultValue ?: return@lz null
        getLanguagePlugin().convertElement(defaultValue, this) as? UExpression
    }

    private val kotlinOrigin = (psi.originalElement as KtLightElement<*, *>).kotlinOrigin

    override fun getContainingFile(): PsiFile? = kotlinOrigin?.containingFile ?: psi.containingFile

    override fun getNameIdentifier() = UastLightIdentifier(psi, kotlinOrigin as KtNamedDeclaration?)

    override val annotations by lz {
        psi.annotations
                .mapNotNull { (it as? KtLightElement<*, *>)?.kotlinOrigin as? KtAnnotationEntry }
                .map { KotlinUAnnotation(it, this) }
    }

    override val uastParameters by lz {
        psi.parameterList.parameters.map { KotlinUParameter(it, this) }
    }

    override val uastAnchor: UElement
        get() = UIdentifier((psi.originalElement as? PsiNameIdentifierOwner)?.nameIdentifier ?: psi.nameIdentifier, this)


    override val uastBody by lz {
        val bodyExpression = when (kotlinOrigin) {
            is KtFunction -> kotlinOrigin.bodyExpression
            is KtProperty -> when {
                psi.isGetter -> kotlinOrigin.getter?.bodyExpression
                psi.isSetter -> kotlinOrigin.setter?.bodyExpression
                else -> null
            }
            else -> null
        } ?: return@lz null

        getLanguagePlugin().convertElement(bodyExpression, this) as? UExpression
    }

    override val isOverride: Boolean
        get() = (kotlinOrigin as? KtCallableDeclaration)?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

    override fun getBody(): PsiCodeBlock? = super.getBody()

    override fun getOriginalElement(): PsiElement? = super.getOriginalElement()

    override fun equals(other: Any?) = other is KotlinUMethod && psi == other.psi

    override fun hashCode() = psi.hashCode()

    companion object {
        fun create(psi: KtLightMethod, containingElement: UElement?) = KotlinUMethod(psi, containingElement)
    }
}