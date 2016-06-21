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

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.types.JeDeclaredErrorType
import org.jetbrains.kotlin.java.model.types.JeDeclaredType
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class JeAnnotationMirror(val psi: PsiAnnotation) : AnnotationMirror {
    override fun getAnnotationType(): DeclaredType? {
        val psiClass = resolveAnnotationClass() ?: return JeDeclaredErrorType
        return JeDeclaredType(PsiTypesUtil.getClassType(psiClass), psiClass)
    }

    override fun getElementValues(): Map<out ExecutableElement, AnnotationValue> {
        val annotationClass = resolveAnnotationClass() ?: return emptyMap()
        
        return mutableMapOf<ExecutableElement, AnnotationValue>().apply {
            for (attribute in psi.parameterList.attributes) {
                val attributeValue = attribute.value ?: continue
                val method = annotationClass.methods.firstOrNull {
                    it is PsiAnnotationMethod && it.name == attribute.name 
                } ?: return emptyMap()
                put(JeMethodExecutableElement(method), JeAnnotationValue(attributeValue))
            }
        }
    }

    fun getAllElementValues(): Map<out ExecutableElement, AnnotationValue> {
        val annotationClass = resolveAnnotationClass() ?: return emptyMap()

        return mutableMapOf<ExecutableElement, AnnotationValue>().apply {
            for (method in annotationClass.methods) {
                method as? PsiAnnotationMethod ?: continue
                val attributeValue = psi.findAttributeValue(method.name) ?: method.defaultValue ?: continue
                put(JeMethodExecutableElement(method), JeAnnotationValue(attributeValue))
            }
        }
    }
    
    private fun resolveAnnotationClass() = psi.nameReferenceElement?.resolve() as? PsiClass
}