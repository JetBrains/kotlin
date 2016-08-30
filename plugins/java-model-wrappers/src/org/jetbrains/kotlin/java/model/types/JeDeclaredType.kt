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
import com.intellij.psi.impl.light.LightClassReferenceExpression
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.internal.isStatic
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVisitor

fun createDeclaredType(psiClass: PsiClass, typeArgs: List<PsiType>): PsiClassReferenceType? {
    val args = typeArgs.toTypedArray()
    val text = (psiClass.name ?: return null) + typeArgs.joinToString(prefix = "<", postfix = ">") { it.canonicalText }
    return PsiClassReferenceType(object : LightClassReferenceExpression(psiClass.manager, text, psiClass) {
        override fun getTypeParameters() = args
    }, LanguageLevel.JDK_1_8)
}

class JeDeclaredType(
        override val psiType: PsiClassType,
        val psiClass: PsiClass,
        val enclosingDeclaredType: DeclaredType? = null
) : JePsiType(), JeTypeWithManager, DeclaredType {
    override fun getKind() = TypeKind.DECLARED
    
    override fun <R : Any?, P : Any?> accept(v: TypeVisitor<R, P>, p: P) = v.visitDeclared(this, p)

    override val psiManager: PsiManager
        get() = psiClass.manager

    override fun getTypeArguments(): List<TypeMirror> {
        return when (psiType) {
            is PsiClassReferenceType -> psiType.parameters.map { it.toJeType(psiManager) }
            is PsiClassType -> {
                val substitutor = psiType.resolveGenerics().substitutor
                val psiClass = psiType.resolve() ?: return psiType.parameters.map { it.toJeType(psiManager) }

                val args = mutableListOf<TypeMirror>()
                for (typeParameter in psiClass.typeParameters) {
                    val substitutedParameter = substitutor.substitute(typeParameter)
                    if (substitutedParameter != null)
                        args += substitutedParameter.toJeType(psiManager)
                    else
                        args += JeTypeVariableType(PsiTypesUtil.getClassType(typeParameter), typeParameter)
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
        return PsiTypesUtil.getClassType(psiClass).toJeType(psiManager)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as? JeDeclaredType ?: return false
        
        return enclosingType == other.enclosingType
               && psiClass == other.psiClass
               && typeArguments == other.typeArguments
    }
    
    override fun hashCode(): Int {
        var result = enclosingType.hashCode()
        result = 31 * result + psiClass.hashCode()
        result = 31 * result + typeArguments.hashCode()
        return result
    }
    
    override fun toString() = psiType.getCanonicalText(false)
}