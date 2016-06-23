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

package org.jetbrains.kotlin.java.model.types

import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import javax.lang.model.type.*

class JeClassInitializerExecutableTypeMirror(val initializer: PsiClassInitializer) : JeTypeMirror, JeTypeWithManager, ExecutableType {
    override fun getKind() = TypeKind.EXECUTABLE
    
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitExecutable(this, p)

    override val psiManager: PsiManager
        get() = initializer.manager

    override fun getReturnType() = CustomJeNoneType(TypeKind.VOID)

    override fun getReceiverType() = JeNoneType

    override fun getThrownTypes() = emptyList<TypeMirror>()

    override fun getParameterTypes() = emptyList<TypeMirror>()

    override fun getTypeVariables() = emptyList<TypeVariable>()

    override fun toString() = (initializer.containingClass?.qualifiedName?.let { it + "." } ?: "") +
                              (if (initializer.hasModifierProperty(PsiModifier.STATIC)) "<clinit>" else "<init>")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return initializer == (other as JeClassInitializerExecutableTypeMirror).initializer
    }

    override fun hashCode() = initializer.hashCode()
}