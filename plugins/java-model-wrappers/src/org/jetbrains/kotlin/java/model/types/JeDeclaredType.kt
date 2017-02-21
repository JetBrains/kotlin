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

import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.*
import com.intellij.psi.impl.PsiSubstitutorImpl
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiImmediateClassType
import org.jetbrains.kotlin.annotation.processing.impl.toDisposable
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.internal.getTypeWithTypeParameters
import org.jetbrains.kotlin.java.model.internal.isStatic
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVisitor

fun createImmediateClassType(psiClass: PsiClass, typeArgs: List<PsiType>): PsiImmediateClassType? {
    val typeParameters = psiClass.typeParameters
    assert(typeParameters.size == typeArgs.size) { "Type parameters size: ${typeParameters.size}, type args size: ${typeArgs.size}" }

    val parametersMap = typeParameters.zip(typeArgs).toMap()
    val substitutor = PsiSubstitutorImpl.createSubstitutor(parametersMap)
    return PsiImmediateClassType(psiClass, substitutor, LanguageLevel.JDK_1_8)
}

class JeDeclaredType(
        psiType: PsiClassType,
        psiClass: PsiClass,
        val enclosingDeclaredType: DeclaredType? = null,
        val isRaw: Boolean = false
) : JePsiTypeBase<PsiClassType>(psiType, psiClass.manager), DeclaredType {
    private val disposablePsiClass = psiClass.toDisposable()

    val psiClass: PsiClass
        get() = disposablePsiClass()

    // JeElementRegistry registration is done in JePsiType
    override fun dispose() {
        super.dispose()
        disposablePsiClass.dispose()
    }

    override fun getKind() = TypeKind.DECLARED
    
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitDeclared(this, p)

    override val psiManager: PsiManager
        get() = psiClass.manager

    override fun getTypeArguments(): List<TypeMirror> {
        return when (psiType) {
            is PsiClassReferenceType -> psiType.parameters.map { it.toJeType(psiManager) }
            is PsiClassType -> {
                if (isRaw) return emptyList()
                
                val substitutor = psiType.resolveGenerics().substitutor
                val psiClass = psiType.resolve() ?: return psiType.parameters.map { it.toJeType(psiManager) }

                val args = mutableListOf<TypeMirror>()
                for (typeParameter in psiClass.typeParameters) {
                    val substitutedParameter = substitutor.substitute(typeParameter)
                    if (substitutedParameter != null)
                        args += substitutedParameter.toJeType(psiManager)
                    else
                        args += JeTypeVariableType(typeParameter.getTypeWithTypeParameters(), typeParameter)
                }

                args
            }
            else -> emptyList()
        }
    }

    override fun asElement() = JeTypeElement(psiClass)

    override fun getEnclosingType(): TypeMirror {
        if (!psiClass.isStatic) return JeNoneType
        
        if (enclosingDeclaredType != null) return enclosingDeclaredType
        
        val psiClass = psiClass.containingClass ?: return JeNoneType
        return psiClass.getTypeWithTypeParameters().toJeType(psiManager)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        other as? JeDeclaredType ?: return false
        
        return enclosingType == other.enclosingType
               && psiClass == other.psiClass
               && typeArguments == other.typeArguments
               && isRaw == other.isRaw
    }
    
    override fun hashCode(): Int {
        var result = enclosingType.hashCode()
        result = 31 * result + psiClass.hashCode()
        result = 31 * result + typeArguments.hashCode()
        result = 31 * result + isRaw.hashCode()
        return result
    }
    
    override fun toString() = buildString {
        append(psiClass.qualifiedName ?: psiClass.name)
        val typeArgs = typeArguments
        if (typeArgs.isNotEmpty()) {
            append('<')
            append(typeArguments.joinToString(","))
            append('>')
        }
    }
}