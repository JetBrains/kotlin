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

import org.jetbrains.kotlin.java.model.elements.JeTypeParameterElement
import com.intellij.psi.*
import org.jetbrains.kotlin.annotation.processing.impl.toDisposable
import org.jetbrains.kotlin.java.model.toJeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.TypeVisitor

class JeTypeVariableType(
        psiType: PsiClassType,
        parameter: PsiTypeParameter
) : JePsiTypeBase<PsiClassType>(psiType, parameter.manager), TypeVariable {
    private val disposableParameter = parameter.toDisposable()

    val parameter: PsiTypeParameter
        get() = disposableParameter()

    // JeElementRegistry registration is done in JeDisposablePsiElementOwner
    override fun dispose() {
        super.dispose()
        disposableParameter.dispose()
    }

    override fun getKind() = TypeKind.TYPEVAR
    
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitTypeVariable(this, p)

    override val psiManager: PsiManager
        get() = parameter.manager

    override fun getLowerBound(): TypeMirror? {
        //TODO support captured lower bounds
        return JeNullType
    }

    override fun getUpperBound(): TypeMirror? {
        val superTypes = parameter.superTypes
        return if (superTypes.size == 1) {
            superTypes.first().toJeType(psiManager)
        } else {
            PsiIntersectionType.createIntersection(*superTypes).toJeType(psiManager)
        }
    }

    override fun asElement() = JeTypeParameterElement(parameter, parameter.owner.toJeElement())

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return psiType == (other as? JeTypeVariableType)?.psiType
    }

    override fun toString() = parameter.name ?: "<none>"

    override fun hashCode() = psiType.hashCode()
}