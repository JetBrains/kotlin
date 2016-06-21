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

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.elements.getReceiverTypeMirror
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVisitor

class JeMethodExecutableTypeMirror(
        val method: PsiMethod,
        val signature: MethodSignature? = null,
        val returnType: PsiType? = null
) : JeTypeBase(), ExecutableType {
    override fun getKind() = TypeKind.EXECUTABLE
    
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitExecutable(this, p)

    override fun getReturnType() = (returnType ?: method.returnType)?.let { it.toJeType() } ?: CustomJeNoneType(TypeKind.VOID)

    override fun getReceiverType() = method.getReceiverTypeMirror()

    override fun getThrownTypes() = method.throwsList.referencedTypes.map { it.toJeType() }

    override fun getParameterTypes(): List<TypeMirror> {
        signature?.parameterTypes?.let { types -> return types.map { it.toJeType() } }
        return method.parameterList.parameters.map { it.type.toJeType() }
    }

    override fun getTypeVariables(): List<JeTypeVariableType> {
        val typeParameters = signature?.typeParameters ?: method.typeParameters
        return typeParameters.map { JeTypeVariableType(PsiTypesUtil.getClassType(it), it) }
    }

    override fun toString() = (method.containingClass?.qualifiedName?.let { it + "." } ?: "") + method.name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return method == (other as JeMethodExecutableTypeMirror).method
    }

    override fun hashCode() = method.hashCode()
}