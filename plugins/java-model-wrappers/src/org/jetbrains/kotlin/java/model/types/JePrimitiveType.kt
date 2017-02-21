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

import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor

class JePrimitiveType(override val psiType: PsiPrimitiveType) : JePsiType, PrimitiveType {
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitPrimitive(this, p)
    
    override fun getKind() = when (psiType) {
        PsiType.BYTE -> TypeKind.BYTE
        PsiType.CHAR -> TypeKind.CHAR
        PsiType.DOUBLE -> TypeKind.DOUBLE
        PsiType.FLOAT -> TypeKind.FLOAT
        PsiType.INT -> TypeKind.INT
        PsiType.LONG -> TypeKind.LONG
        PsiType.SHORT -> TypeKind.SHORT
        PsiType.BOOLEAN -> TypeKind.BOOLEAN
        PsiType.VOID -> TypeKind.VOID
        else -> TypeKind.ERROR
    }

    override fun toString() = psiType.canonicalText

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return psiType == (other as? JePrimitiveType)?.psiType
    }

    override fun hashCode() = psiType.hashCode()
}