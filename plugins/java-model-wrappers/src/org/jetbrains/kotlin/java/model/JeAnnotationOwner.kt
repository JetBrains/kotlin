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

package org.jetbrains.kotlin.java.model

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.java.model.elements.JeAnnotationMirror
import org.jetbrains.kotlin.java.model.internal.KotlinAnnotationProxyMaker
import org.jetbrains.kotlin.java.model.internal.getAnnotationsWithInherited
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import java.lang.reflect.Array as RArray

interface JeAnnotationOwner : JeElement {
    override val psi: PsiModifierListOwner
    
    override fun getAnnotationMirrors() = psi.getAnnotationsWithInherited().map(::JeAnnotationMirror)
    
    override fun <A : Annotation> getAnnotation(annotationClass: Class<A>): A? {
        return getAnnotationsByType(annotationClass, onlyFirst = true).firstOrNull()
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation> getAnnotationsByType(annotationClass: Class<A>): Array<A> {
        val annotations = getAnnotationsByType(annotationClass, onlyFirst = false)
        
        return (RArray.newInstance(annotationClass, annotations.size) as Array<A>).apply {
            annotations.forEachIndexed { i, annotation -> RArray.set(this, i, annotation) }
        }
    }
    
    private fun <A : Annotation> getAnnotationsByType(annotationClass: Class<A>, onlyFirst: Boolean): List<A> {
        if (!annotationClass.isAnnotation) {
            throw IllegalArgumentException("Not an annotation class: " + annotationClass)
        }

        val annotationFqName = annotationClass.canonicalName
        
        val allAnnotations = psi.getAnnotationsWithInherited().filter { it.qualifiedName == annotationFqName }
        if (allAnnotations.isEmpty()) return emptyList()
        
        val annotations = if (onlyFirst) listOf(allAnnotations.first()) else allAnnotations

        val annotationDeclarations = annotations.map { it to it.nameReferenceElement?.resolve() as? PsiClass }.filter { it.second != null }

        @Suppress("UNCHECKED_CAST")
        val annotationProxies = annotationDeclarations.map {
            val (annotation, annotationDeclaration) = it
            KotlinAnnotationProxyMaker(annotation, annotationDeclaration!!, annotationClass).generate() as? A
        }

        @Suppress("UNCHECKED_CAST")
        return annotationProxies as List<A>
    }
}

interface JeNoAnnotations : Element {
    override fun getAnnotationMirrors() = emptyList<AnnotationMirror>()

    override fun <A : Annotation> getAnnotation(annotationClass: Class<A>?): Nothing? {
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A>): Array<A> {
        return RArray.newInstance(annotationType, 0) as Array<A>
    }
}