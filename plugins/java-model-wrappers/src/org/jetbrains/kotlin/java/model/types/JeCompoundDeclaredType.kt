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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVisitor

class JeCompoundDeclaredType(
        val psiClass: PsiClass,
        val baseType: TypeElement,
        val enclosingTypeMirror: TypeMirror?,
        val typeArgs: List<PsiType>,
        val typeArgMirrors: List<TypeMirror>
) : JeTypeBase(), DeclaredType {
    override fun getKind() = TypeKind.DECLARED
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitDeclared(this, p)
    
    override fun getTypeArguments() = typeArgMirrors
    override fun asElement() = baseType
    override fun getEnclosingType() = enclosingTypeMirror ?: JeNoneType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as JeCompoundDeclaredType

        if (baseType != other.baseType) return false
        if (enclosingTypeMirror != other.enclosingTypeMirror) return false
        if (typeArgs != other.typeArgs) return false

        return true
    }

    override fun hashCode(): Int{
        var result = baseType.hashCode()
        result = 31 * result + (enclosingTypeMirror?.hashCode() ?: 0)
        result = 31 * result + typeArgs.hashCode()
        return result
    }

    override fun toString(): String {
        return baseType.toString() + typeArgs.joinToString(prefix = "<", postfix = ">")
    }
}