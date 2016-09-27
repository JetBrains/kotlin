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

import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.internal.getTypeWithTypeParameters
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.TypeParameterElement

class JeTypeParameterElement(
        psi: PsiTypeParameter,
        val parent: JeElement?
) : JeAbstractElement<PsiTypeParameter>(psi), TypeParameterElement, JeAnnotationOwner, JeModifierListOwner {
    override fun getSimpleName() = JeName(psi.name)

    override fun getEnclosingElement() = parent

    override fun getKind() = ElementKind.TYPE_PARAMETER

    override fun asType() = psi.getTypeWithTypeParameters().toJeType(psi.manager)

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitTypeParameter(this, p)

    override fun getEnclosedElements() = emptyList<Element>()
    
    override fun getBounds() = psi.superTypes.map { it.toJeType(psi.manager) }

    override fun getGenericElement() = parent

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return psi == (other as JeTypeParameterElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.name ?: "T"
}