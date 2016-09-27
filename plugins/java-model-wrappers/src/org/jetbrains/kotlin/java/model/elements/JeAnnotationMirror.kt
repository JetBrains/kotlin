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
import org.jetbrains.kotlin.java.model.JeDisposablePsiElementOwner
import org.jetbrains.kotlin.java.model.internal.getTypeWithTypeParameters
import org.jetbrains.kotlin.java.model.types.JeDeclaredErrorType
import org.jetbrains.kotlin.java.model.types.JeDeclaredType
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class JeAnnotationMirror(psi: PsiAnnotation) : JeDisposablePsiElementOwner<PsiAnnotation>(psi), AnnotationMirror {
    override fun getAnnotationType(): DeclaredType? {
        val psiClass = resolveAnnotationClass() ?: return JeDeclaredErrorType
        return JeDeclaredType(psiClass.getTypeWithTypeParameters(), psiClass)
    }

    override fun getElementValues(): Map<out ExecutableElement, AnnotationValue> = getElementValues(false)
    
    fun getAllElementValues(): Map<out ExecutableElement, AnnotationValue> = getElementValues(true)

    private fun getElementValues(withDefaults: Boolean): Map<out ExecutableElement, AnnotationValue> {
        val annotationClass = resolveAnnotationClass() ?: return emptyMap()

        return mutableMapOf<ExecutableElement, AnnotationValue>().apply {
            for (method in annotationClass.methods) {
                method as? PsiAnnotationMethod ?: continue
                val returnType = method.returnType ?: continue
                
                val attributeValue = psi.findDeclaredAttributeValue(method.name) 
                                     ?: (if (withDefaults) method.defaultValue else null)
                                     ?: continue
                
                val annotationValue = when {
                    returnType is PsiArrayType && attributeValue !is PsiArrayInitializerMemberValue -> 
                        JeSingletonArrayAnnotationValue(attributeValue)
                    else -> JeAnnotationValue(attributeValue) 
                }
                
                put(JeMethodExecutableElement(method), annotationValue)
            }
        }
    }
    
    private fun resolveAnnotationClass() = psi.nameReferenceElement?.resolve() as? PsiClass
}