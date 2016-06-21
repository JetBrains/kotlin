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

import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.JeAnnotationOwner
import org.jetbrains.kotlin.java.model.JeElement
import org.jetbrains.kotlin.java.model.JeModifierListOwner
import org.jetbrains.kotlin.java.model.JeName
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.TypeParameterElement

class JeTypeParameterElement(
        override val psi: PsiTypeParameter,
        val parent: JeElement?
) : JeElement(), TypeParameterElement, JeAnnotationOwner, JeModifierListOwner {
    override fun getSimpleName() = JeName(psi.name)

    override fun getEnclosingElement() = parent

    override fun getKind() = ElementKind.TYPE_PARAMETER

    override fun asType() = PsiTypesUtil.getClassType(psi).toJeType()

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitTypeParameter(this, p)

    override fun getEnclosedElements() = emptyList<Element>()
    
    override fun getBounds() = psi.superTypes.map { it.toJeType() }

    override fun getGenericElement() = parent

    override val annotationOwner: PsiAnnotationOwner?
        get() = psi.modifierList

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return psi == (other as JeTypeParameterElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.name ?: "T"
}