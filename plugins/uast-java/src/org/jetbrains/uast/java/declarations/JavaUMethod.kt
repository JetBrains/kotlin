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

package org.jetbrains.uast.java

import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.*
import org.jetbrains.uast.java.internal.JavaUElementWithComments

open class JavaUMethod(
        psi: PsiMethod,
        override val containingElement: UElement?
) : UMethod, JavaUElementWithComments, PsiMethod by psi {
    override val psi = unwrap<UMethod, PsiMethod>(psi)
    
    override val uastBody by lz {
        val body = psi.body ?: return@lz null
        getLanguagePlugin().convertElement(body, this) as? UExpression
    }

    override val annotations by lz { psi.annotations.map { JavaUAnnotation(it, this) } }
    
    override val uastParameters by lz {
        psi.parameterList.parameters.map { JavaUParameter(it, this) }
    }

    override val isOverride: Boolean
        get() = psi.modifierList.findAnnotation("java.lang.Override") != null

    override val uastAnchor: UElement
        get() = UIdentifier((psi.originalElement as? PsiNameIdentifierOwner)?.nameIdentifier ?: psi.nameIdentifier, this)

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()

    companion object {
        fun create(psi: PsiMethod, languagePlugin: UastLanguagePlugin, containingElement: UElement?) = when (psi) {
            is PsiAnnotationMethod -> JavaUAnnotationMethod(psi, languagePlugin, containingElement)
            else -> JavaUMethod(psi, containingElement)
        }
    }
}

class JavaUAnnotationMethod(
        override val psi: PsiAnnotationMethod,
        languagePlugin: UastLanguagePlugin,
        containingElement: UElement?
) : JavaUMethod(psi, containingElement), UAnnotationMethod {
    override val uastDefaultValue by lz {
        val defaultValue = psi.defaultValue ?: return@lz null
        languagePlugin.convertElement(defaultValue, this, null) as? UExpression
    }
}