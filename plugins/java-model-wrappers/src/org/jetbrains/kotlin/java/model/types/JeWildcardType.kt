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

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWildcardType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor
import javax.lang.model.type.WildcardType

class JeWildcardType(override val psiType: PsiWildcardType) : JePsiType(), JeTypeWithManager, WildcardType {
    override fun getKind() = TypeKind.WILDCARD
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitWildcard(this, p)
    
    override fun getSuperBound() = psiType.superBound.toJeType(psiManager)
    override fun getExtendsBound() = psiType.extendsBound.toJeType(psiManager)

    override val psiManager: PsiManager
        get() = psiType.manager

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        if (!super.equals(other)) return false

        return psiType == (other as JeWildcardType).psiType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + psiType.hashCode()
        return result
    }
}