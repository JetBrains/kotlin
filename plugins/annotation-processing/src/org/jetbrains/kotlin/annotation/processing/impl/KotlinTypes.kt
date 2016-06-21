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

package org.jetbrains.kotlin.annotation.processing.impl

import com.intellij.psi.*
import com.intellij.psi.impl.PsiSubstitutorImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.java.model.elements.JeClassInitializerExecutableElement
import org.jetbrains.kotlin.java.model.elements.JeMethodExecutableElement
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.elements.JeVariableElement
import org.jetbrains.kotlin.java.model.types.*
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.Types

class KotlinTypes(val javaPsiFacade: JavaPsiFacade, val scope: GlobalSearchScope) : Types {
    override fun contains(t1: TypeMirror, t2: TypeMirror): Boolean {
        t1 as? JeAbstractType ?: return false
        t2 as? JeAbstractType ?: return false
        
        val classType = t1.psiType as? PsiClassType ?: return false
        return t2.psiType in classType.parameters
    }

    override fun getArrayType(componentType: TypeMirror): ArrayType {
        val psiType = (componentType as? JeAbstractType)?.psiType
                ?: throw IllegalArgumentException("Invalid component type: $componentType")
        return JeArrayType(PsiArrayType(psiType))
    }

    override fun isAssignable(t1: TypeMirror, t2: TypeMirror): Boolean {
        fun error(t: TypeMirror?): Nothing = throw IllegalArgumentException("Invalid type: $t")
        if (t1 is ExecutableType || t1 is NoType) error(t1)
        if (t2 is ExecutableType || t2 is NoType) error(t2)
        
        t1 as? JeAbstractType ?: return false
        t2 as? JeAbstractType ?: return false
        return t1.psiType.isAssignableFrom(t2.psiType)
    }

    override fun getNullType() = JeNullType

    override fun getWildcardType(extendsBound: TypeMirror, superBound: TypeMirror): JeWildcardTypeWithBounds {
        return JeWildcardTypeWithBounds(extendsBound, superBound)
    }

    override fun unboxedType(t: TypeMirror): PrimitiveType? {
        fun error(): Nothing = throw IllegalArgumentException("This type could not be unboxed: $t")
        t as? JeAbstractType ?: error()
        val unboxedType = PsiPrimitiveType.getUnboxedType(t.psiType) ?: error()
        return unboxedType.toJePrimitiveType()
    }

    override fun getPrimitiveType(kind: TypeKind) = kind.toJePrimitiveType()

    override fun erasure(t: TypeMirror): TypeMirror {
        if (t is NoType) throw IllegalArgumentException("Invalid type: $t")
        t as? JeAbstractType ?: return t
        return TypeConversionUtil.erasure(t.psiType).toJeType()
    }

    override fun directSupertypes(t: TypeMirror): List<TypeMirror> {
        if (t is NoType) throw IllegalArgumentException("Invalid type: $t")
        val psiType = (t as? JeAbstractType)?.psiType as? PsiClassType ?: return emptyList()
        return psiType.superTypes.map { it.toJeType() }
    }

    override fun boxedClass(p: PrimitiveType): TypeElement? {
        p as? JePrimitiveType ?: throw IllegalArgumentException("Unknown type: $p")
        val boxedTypeName = p.psiType.boxedTypeName
        val boxedClass = javaPsiFacade.findClass(boxedTypeName, scope) 
                         ?: throw IllegalStateException("Can't find boxed class $boxedTypeName")
        return JeTypeElement(boxedClass)
    }

    override fun asElement(t: TypeMirror): Element? {
        if (t is JeDeclaredType) {
            return t.asElement()
        } else if (t is JeTypeVariableType) {
            return t.asElement()
        }

        return null
    }

    override fun isSubtype(t1: TypeMirror, t2: TypeMirror): Boolean {
        val psiType1 = (t1 as? JeAbstractType)?.psiType ?: return false
        val psiType2 = (t2 as? JeAbstractType)?.psiType ?: return false
        
        return TypeConversionUtil.isAssignable(psiType2, psiType1, false)
    }

    override fun isSameType(t1: TypeMirror, t2: TypeMirror) = t1 == t2

    override fun getNoType(kind: TypeKind) = CustomJeNoneType(kind)

    override fun getDeclaredType(typeElem: TypeElement, vararg typeArgMirrors: TypeMirror): DeclaredType {
        val psiClass = (typeElem as JeTypeElement).psi
        if (!psiClass.hasTypeParameters() && typeArgMirrors.size > 0) {
            throw IllegalArgumentException("$typeElem has not type parameters")
        }
        
        // Raw type
        if (psiClass.hasTypeParameters() && typeArgMirrors.isEmpty()) {
            return JeDeclaredType(PsiTypesUtil.getClassType(psiClass).rawType(), psiClass)
        }
        
        val typeParameters = psiClass.typeParameters
        if (typeArgMirrors.size != typeParameters.size) {
            throw IllegalArgumentException("$typeElem type parameters count: " +
                    "${typeParameters.size}, received ${typeArgMirrors.size} args")
        }
        
        val typeArgs = typeArgMirrors.mapIndexed {
            i, t -> (t as? JeAbstractType)?.psiType ?: throw IllegalArgumentException("Invalid type argument #$i: $t") 
        }
        
        return JeCompoundDeclaredType(psiClass, typeElem, null, typeArgs, typeArgMirrors.toList())
    }

    override fun getDeclaredType(
            containing: DeclaredType?, 
            typeElem: TypeElement, 
            vararg typeArgMirrors: TypeMirror
    ): DeclaredType? {
        if (containing == null) {
            return getDeclaredType(typeElem, *typeArgMirrors)
        }
        
        val psiClass = (typeElem as JeTypeElement).psi
        if (psiClass.containingClass == null) {
            throw IllegalArgumentException("$typeElem is a top-level class element")
        }
        
        val containingType = (containing as? JeAbstractType)?.psiType as? PsiClassType 
                ?: throw IllegalArgumentException("Illegal containing type: $containing")
        val containingClass = containingType.resolve() 
                ?: throw IllegalArgumentException("Class type can't be resolved: $containing")
        
        if (psiClass.containingClass != containingClass) {
            throw IllegalArgumentException("$containing is not an enclosing element for $typeElem")
        }

        val typeArgs = typeArgMirrors.mapIndexed {
            i, t -> (t as? JeAbstractType)?.psiType ?: throw IllegalArgumentException("Invalid type argument #$i: $t")
        }
        
        return JeCompoundDeclaredType(psiClass, typeElem, containing, typeArgs, typeArgMirrors.toList())
    }

    override fun asMemberOf(containing: DeclaredType, element: Element): TypeMirror {
        val substitutor = when (containing) {
            is JeDeclaredType -> {
                val result = containing.psiType.resolveGenerics()
                if (result.isValidResult) result.substitutor else PsiSubstitutor.EMPTY
            }
            is JeCompoundDeclaredType -> {
                val mapping = containing.psiClass.typeParameters.zip(containing.typeArgs).toMap()
                PsiSubstitutorImpl.createSubstitutor(mapping)
            }
            else -> throw IllegalArgumentException("Invalid containing type: $containing")
        }
        
        return when (element) {
            is JeMethodExecutableElement -> {
                val method = element.psi
                if (method.hasModifierProperty(PsiModifier.STATIC) || !method.hasTypeParameters()) {
                    JeMethodExecutableTypeMirror(method)
                } else {
                    val signature = method.getSignature(substitutor)
                    val returnType = substitutor.substitute(element.psi.returnType)
                    JeMethodExecutableTypeMirror(method, signature, returnType)
                }
            }
            is JeClassInitializerExecutableElement -> element.asType()
            is JeVariableElement -> substitutor.substitute(element.psi.type).toJeType()
            else -> throw IllegalArgumentException("Invalid element type: $element")
        }
    }

    override fun isSubsignature(t1: ExecutableType, t2: ExecutableType): Boolean {
        val m1 = (t1 as JeClassInitializerExecutableTypeMirror).initializer
        val m2 = (t2 as JeClassInitializerExecutableTypeMirror).initializer
        
        if (m1 !is PsiMethod || m2 !is PsiMethod) return false
        if (m1.parameterList.parametersCount != m2.parameterList.parametersCount) return false
        
        for (i in 0..(m1.parameterList.parametersCount - 1)) {
            val p1 = m1.parameterList.parameters[i]
            val p2 = m2.parameterList.parameters[i]
            
            if (!TypeConversionUtil.isAssignable(p2.type, p1.type, false)) {
                return false
            }
        }
        
        return true
    }

    override fun capture(t: TypeMirror): TypeMirror? {
        TODO()
    }
}