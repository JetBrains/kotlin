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

package org.jetbrains.kotlin.java.model.elements

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.internal.calcConstantValue
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.VariableElement

class JeVariableElement(psi: PsiVariable) : JeAbstractElement<PsiVariable>(psi), VariableElement, JeModifierListOwner, JeAnnotationOwner {
    override fun getSimpleName() = JeName(psi.name)

    override fun getEnclosingElement(): JeElement? {
        val psi = psi

        if (psi is PsiParameter) {
            (psi.declarationScope as? PsiMethod)?.let { return JeMethodExecutableElement(it) }
        }
        
        val containingClass = (psi as? PsiMember)?.containingClass ?: PsiTreeUtil.getParentOfType(psi, PsiClass::class.java) 
        return containingClass?.let(::JeTypeElement)
    }

    override fun getConstantValue() = psi.initializer?.calcConstantValue()

    override fun getKind() = when (psi) {
        is PsiField -> ElementKind.FIELD
        is PsiParameter -> ElementKind.PARAMETER
        else -> ElementKind.LOCAL_VARIABLE
    }

    override fun asType() = psi.type.toJeType(psi.manager)

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitVariable(this, p)

    override fun getEnclosedElements() = emptyList<Element>()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return psi == (other as JeVariableElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.name ?: "<unnamed variable>"
}