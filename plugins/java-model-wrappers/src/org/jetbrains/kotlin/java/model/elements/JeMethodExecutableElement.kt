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
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.internal.getTypeWithTypeParameters
import org.jetbrains.kotlin.java.model.internal.isStatic
import org.jetbrains.kotlin.java.model.types.JeMethodExecutableTypeMirror
import org.jetbrains.kotlin.java.model.types.JeNoneType
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

class JeMethodExecutableElement(psi: PsiMethod) : JeAbstractElement<PsiMethod>(psi), ExecutableElement, JeModifierListOwner, JeAnnotationOwner {
    override fun getEnclosingElement() = psi.containingClass?.let(::JeTypeElement)

    override fun getSimpleName(): JeName {
        if (psi.isConstructor) return JeName.INIT
        return JeName(psi.name)
    }

    override fun getThrownTypes() = psi.throwsList.referencedTypes.map { it.toJeType(psi.manager) }

    override fun getTypeParameters() = psi.typeParameters.map { JeTypeParameterElement(it, this) }

    override fun getParameters() = psi.parameterList.parameters.map(::JeVariableElement)

    override fun getDefaultValue(): AnnotationValue? {
        val annotationMethod = psi as? PsiAnnotationMethod ?: return null
        val defaultValue = annotationMethod.defaultValue ?: return null
        return JeAnnotationValue(defaultValue)
    }

    override fun getReturnType() = psi.returnType?.let { it.toJeType(psi.manager) } ?: JeNoneType

    override fun getReceiverType() = psi.getReceiverTypeMirror()
    
    override fun isVarArgs() = psi.isVarArgs

    override fun isDefault() = psi.hasModifierProperty(PsiModifier.DEFAULT)

    override fun getKind() = when {
        psi.isConstructor -> ElementKind.CONSTRUCTOR
        else -> ElementKind.METHOD
    }

    override fun asType() = JeMethodExecutableTypeMirror(psi)

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitExecutable(this, p)

    override fun getEnclosedElements() = emptyList<Element>()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false

        return psi == (other as JeMethodExecutableElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.name
}

fun PsiMethod.getReceiverTypeMirror(): TypeMirror {
    if (isStatic) return JeNoneType

    if (isConstructor) {
        val containingClass = containingClass
        if (containingClass != null && !containingClass.isStatic) {
            containingClass.containingClass?.let {
                return it.getTypeWithTypeParameters().toJeType(manager)
            }
        }

        return JeNoneType
    }

    val containingClass = containingClass ?: return JeNoneType
    return containingClass.getTypeWithTypeParameters().toJeType(manager)

}