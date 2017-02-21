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
import org.jetbrains.kotlin.java.model.JeDisposablePsiElementOwner
import org.jetbrains.kotlin.java.model.internal.isStatic
import javax.lang.model.type.*

class JeClassInitializerExecutableTypeMirror(
        psi: PsiClassInitializer
) : JeDisposablePsiElementOwner<PsiClassInitializer>(psi), JeTypeMirror, JeTypeWithManager, ExecutableType {
    override fun getKind() = TypeKind.EXECUTABLE
    
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitExecutable(this, p)

    override fun getReturnType() = JeVoidType

    override fun getReceiverType() = JeNoneType

    override fun getThrownTypes() = emptyList<TypeMirror>()

    override fun getParameterTypes() = emptyList<TypeMirror>()

    override fun getTypeVariables() = emptyList<TypeVariable>()

    override fun toString() = (psi.containingClass?.qualifiedName?.let { it + "." } ?: "") +
                              (if (psi.isStatic) "<clinit>" else "<instinit>")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        other as? JeClassInitializerExecutableTypeMirror ?: return false
        return psi == other.psi
    }

    override fun hashCode() = psi.hashCode()
}