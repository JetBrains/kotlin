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

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import org.jetbrains.uast.*
import org.jetbrains.uast.java.internal.JavaUElementWithComments

abstract class AbstractJavaUClass : UClass, JavaUElementWithComments {
    override val uastDeclarations by lz {
        mutableListOf<UDeclaration>().apply {
            addAll(uastFields)
            addAll(uastInitializers)
            addAll(uastMethods)
            addAll(uastNestedClasses)
        }
    }

    override val uastAnchor: UElement?
        get() = UIdentifier(psi.nameIdentifier, this)

    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, this) } }
    
    override val uastFields: List<UVariable> by lz { psi.fields.map { getLanguagePlugin().convert<UVariable>(it, this) } }
    override val uastInitializers: List<UClassInitializer> by lz { psi.initializers.map { getLanguagePlugin().convert<UClassInitializer>(it, this) } }
    override val uastMethods: List<UMethod> by lz { psi.methods.map { getLanguagePlugin().convert<UMethod>(it, this) } }
    override val uastNestedClasses: List<UClass> by lz { psi.innerClasses.map { getLanguagePlugin().convert<UClass>(it, this) } }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()
}

class JavaUClass private constructor(psi: PsiClass, override val containingElement: UElement?) : AbstractJavaUClass(), PsiClass by psi {
    override val psi = unwrap<UClass, PsiClass>(psi)

    companion object {
        fun create(psi: PsiClass, containingElement: UElement?): UClass {
            return if (psi is PsiAnonymousClass) 
                JavaUAnonymousClass(psi, containingElement)
            else
                JavaUClass(psi, containingElement)
        }
    }
}

class JavaUAnonymousClass(
        psi: PsiAnonymousClass,
        override val containingElement: UElement?
) : AbstractJavaUClass(), UAnonymousClass, PsiAnonymousClass by psi {
    override val psi: PsiAnonymousClass = unwrap<UAnonymousClass, PsiAnonymousClass>(psi)
}