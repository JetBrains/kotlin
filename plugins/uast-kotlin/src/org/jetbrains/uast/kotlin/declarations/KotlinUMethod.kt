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

import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUMethod
import org.jetbrains.uast.kotlin.lz
import org.jetbrains.uast.kotlin.unwrap

open class KotlinUMethod(
        psi: KtLightMethod,
        containingElement: UElement?
) : JavaUMethod(psi, containingElement) {
    override val psi: KtLightMethod = unwrap<UMethod, KtLightMethod>(psi)
    private val kotlinOrigin = (psi.originalElement as KtLightElement<*, *>).kotlinOrigin

    override val uastBody by lz {
        val bodyExpression = (kotlinOrigin as? KtFunction)?.bodyExpression ?: return@lz null
        getLanguagePlugin().convertElement(bodyExpression, this) as? UExpression
    }

    override val isOverride: Boolean
        get() = (kotlinOrigin as? KtCallableDeclaration)?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

    companion object {
        fun create(psi: KtLightMethod, containingElement: UElement?) = when (psi) {
            is KtLightMethodImpl.KtLightAnnotationMethod -> KotlinUAnnotationMethod(psi, containingElement)
            else -> KotlinUMethod(psi, containingElement)
        }
    }
}

class KotlinUAnnotationMethod(
        override val psi: KtLightMethodImpl.KtLightAnnotationMethod,
        containingElement: UElement?
) : KotlinUMethod(psi, containingElement), UAnnotationMethod {
    override val uastDefaultValue by lz {
        val annotationParameter = psi.kotlinOrigin as? KtParameter ?: return@lz null
        val defaultValue = annotationParameter.defaultValue ?: return@lz null
        getLanguagePlugin().convertElement(defaultValue, this) as? UExpression
    }
}