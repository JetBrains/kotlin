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

import com.intellij.psi.PsiIntersectionType
import com.intellij.psi.PsiManager
import javax.lang.model.type.IntersectionType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor

class JeIntersectionType(
        psiType: PsiIntersectionType,
        psiManager: PsiManager,
        private val isRaw: Boolean
) : JePsiTypeBase<PsiIntersectionType>(psiType, psiManager), IntersectionType {
    override fun getKind() = TypeKind.INTERSECTION
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitIntersection(this, p)

    override fun getBounds() = psiType.superTypes.map { it.toJeType(psiManager, isRaw = isRaw) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        other as? JeIntersectionType ?: return false

        return bounds == other.bounds
               && isRaw == other.isRaw
    }

    override fun hashCode(): Int {
        var result = bounds.hashCode()
        result = 31 * result + isRaw.hashCode()
        return result
    }

    override fun toString() = bounds.joinToString("&")
}