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

import com.intellij.psi.PsiCapturedWildcardType
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWildcardType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor
import javax.lang.model.type.WildcardType

class JeWildcardType(
        psiType: PsiWildcardType,
        private val isRaw: Boolean
) : JePsiTypeBase<PsiWildcardType>(psiType, psiType.manager), WildcardType {
    override fun getKind() = TypeKind.WILDCARD
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitWildcard(this, p)
    
    override fun getSuperBound() = psiType.superBound.toJeType(psiManager, isRaw = isRaw)
    override fun getExtendsBound() = psiType.extendsBound.toJeType(psiManager, isRaw = isRaw)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        other as? JeWildcardType ?: return false

        return superBound == other.superBound
               && extendsBound == other.extendsBound
               && isRaw == other.isRaw
    }

    override fun hashCode(): Int {
        var result = superBound.hashCode()
        result = 31 * result + extendsBound.hashCode()
        result = 31 * result + isRaw.hashCode()
        return result
    }

    override fun toString() = when {
        psiType.isExtends -> "? extends $extendsBound"
        psiType.isSuper -> "? super $superBound"
        else -> "?"
    }
}

class JeCapturedWildcardType(
        psiType: PsiCapturedWildcardType,
        psiManager: PsiManager,
        private val isRaw: Boolean
) : JePsiTypeBase<PsiCapturedWildcardType>(psiType, psiManager), WildcardType {
    override fun getKind() = TypeKind.WILDCARD
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitWildcard(this, p)

    override fun getSuperBound() = psiType.lowerBound.toJeType(psiManager, isRaw = isRaw)
    override fun getExtendsBound() = psiType.upperBound.toJeType(psiManager, isRaw = isRaw)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        other as? JeCapturedWildcardType ?: return false

        return superBound == other.superBound
               && extendsBound == other.extendsBound
               && isRaw == other.isRaw
    }

    override fun hashCode(): Int {
        var result = superBound.hashCode()
        result = 31 * result + extendsBound.hashCode()
        result = 31 * result + isRaw.hashCode()
        return result
    }

    override fun toString() = when {
        psiType.wildcard.isSuper -> "? extends $extendsBound"
        psiType.wildcard.isExtends -> "? super $superBound"
        else -> "?"
    }
}