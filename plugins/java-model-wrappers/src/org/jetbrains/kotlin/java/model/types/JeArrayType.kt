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

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiManager
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor

class JeArrayType(override val psiType: PsiArrayType, override val psiManager: PsiManager) : JePsiType(), JeTypeWithManager, ArrayType {
    override fun getKind() = TypeKind.ARRAY
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitArray(this, p)
    override fun getComponentType() = psiType.componentType.toJeType(psiManager)
    
    override fun toString() = psiType.getCanonicalText(false)
    
    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return componentType == (other as? JeArrayType)?.componentType
    }

    override fun hashCode() = componentType.hashCode()
}