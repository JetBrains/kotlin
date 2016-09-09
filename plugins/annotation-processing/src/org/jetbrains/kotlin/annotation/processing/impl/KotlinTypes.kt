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
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.*
import org.jetbrains.kotlin.java.model.JeElement
import org.jetbrains.kotlin.java.model.elements.JeClassInitializerExecutableElement
import org.jetbrains.kotlin.java.model.elements.JeMethodExecutableElement
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.elements.JeVariableElement
import org.jetbrains.kotlin.java.model.types.*
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.Types

class KotlinTypes(val javaPsiFacade: JavaPsiFacade, val psiManager: PsiManager, val scope: GlobalSearchScope) : Types {
    override fun contains(containing: TypeMirror, contained: TypeMirror): Boolean {
        assertKindNot(containing, TypeKind.PACKAGE, TypeKind.EXECUTABLE)
        assertKindNot(contained, TypeKind.PACKAGE, TypeKind.EXECUTABLE)

        assertJeType(containing); containing as JePsiType
        assertJeType(contained); contained as JePsiType
        
        fun L(type: PsiType): PsiType = when {
            type is PsiWildcardType && type.isSuper -> type.superBound
            else -> type
        }
        
        fun U(type: PsiType): PsiType = when {
            type is PsiWildcardType && type.isExtends -> type.extendsBound
            else -> type
        }
        
        return when (containing) {
            is JeArrayType, is JePrimitiveType -> containing == contained
            else -> {
                !GenericsUtil.checkNotInBounds(L(contained.psiType), L(containing.psiType), false) 
                        && !GenericsUtil.checkNotInBounds(U(contained.psiType), U(containing.psiType), false)
            }
        }
    }

    override fun getArrayType(componentType: TypeMirror): ArrayType {
        if (componentType is ExecutableType || componentType is NoType) error(componentType)
        assertJeType(componentType); componentType as JePsiType
        
        return JeArrayType(PsiArrayType(componentType.psiType), psiManager, isRaw = false)
    }

    override fun isAssignable(t1: TypeMirror, t2: TypeMirror): Boolean {
        assertKindNot(t1, TypeKind.PACKAGE, TypeKind.EXECUTABLE, TypeKind.NONE)
        assertKindNot(t2, TypeKind.PACKAGE, TypeKind.EXECUTABLE, TypeKind.NONE)

        t1 as? JePsiType ?: return false
        t2 as? JePsiType ?: return false
        return t2.psiType.isAssignableFrom(t1.psiType)
    }

    override fun getNullType() = JeNullType

    override fun getWildcardType(extendsBound: TypeMirror?, superBound: TypeMirror?): JeWildcardType {
        if (extendsBound != null && superBound != null) {
            throw IllegalArgumentException("Both extendsBound and superBound should not be specified.")
        }
        
        if (extendsBound != null && extendsBound !is JePsiType) illegalArg("extendsBound should have PsiType")
        if (superBound != null && superBound !is JePsiType) illegalArg("superBound should have PsiType")
        
        return JeWildcardType(if (extendsBound != null) {
            PsiWildcardType.createExtends(psiManager, (extendsBound as JePsiType).psiType)
        } else if (superBound != null) {
            PsiWildcardType.createSuper(psiManager, (superBound as JePsiType).psiType)
        } else {
            PsiWildcardType.createUnbounded(psiManager)
        }, isRaw = false)
    }

    override fun unboxedType(t: TypeMirror): PrimitiveType? {
        fun error(): Nothing = throw IllegalArgumentException("This type could not be unboxed: $t")
        t as? JePsiType ?: error()
        val unboxedType = PsiPrimitiveType.getUnboxedType(t.psiType) ?: error()
        return unboxedType.toJePrimitiveType()
    }

    override fun getPrimitiveType(kind: TypeKind) = kind.toJePrimitiveType()

    override fun erasure(t: TypeMirror): TypeMirror {
        if (t.kind == TypeKind.PACKAGE) throw IllegalArgumentException("Invalid type: $t")
        return when (t) {
            is JeTypeVariableType -> TypeConversionUtil.typeParameterErasure(t.parameter).toJeType(t.psiManager, isRaw = true)
            is JePsiType -> TypeConversionUtil.erasure(t.psiType).toJeType(psiManager, isRaw = true)
            is JeMethodExecutableTypeMirror -> {
                val oldSignature = t.signature
                val parameterTypes = oldSignature?.parameterTypes?.toList() ?: t.psi.parameterList.parameters.map { it.type }
                val newSignature = MethodSignatureUtil.createMethodSignature(
                        oldSignature?.name ?: t.psi.name,
                        parameterTypes.map { TypeConversionUtil.erasure(it) }.toTypedArray(),
                        emptyArray(),
                        PsiSubstitutor.EMPTY,
                        oldSignature?.isConstructor ?: t.psi.isConstructor)
                JeMethodExecutableTypeMirror(
                        t.psi, newSignature,
                        TypeConversionUtil.erasure(t.returnType ?: t.psi.returnType), isRaw = true)
            }
            else -> t
        }
    }

    override fun directSupertypes(t: TypeMirror): List<TypeMirror> {
        if (t is NoType || t is ExecutableType) throw IllegalArgumentException("Invalid type: $t")

        if (t is JeDeclaredType && t.psiType is PsiImmediateClassType) {
            return t.psiClass.superTypes.map { it.toJeType(psiManager) }
        }

        val psiType = (t as? JePsiType)?.psiType as? PsiClassType ?: return emptyList()
        return psiType.superTypes.map { it.toJeType(psiManager) }
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
        val psiType1 = (t1 as? JePsiType)?.psiType ?: return false
        val psiType2 = (t2 as? JePsiType)?.psiType ?: return false
        
        return TypeConversionUtil.isAssignable(psiType2, psiType1, false)
    }

    override fun isSameType(t1: TypeMirror, t2: TypeMirror) = t1 == t2
    
    override fun getNoType(kind: TypeKind) = when (kind) {
        TypeKind.VOID -> JeVoidType
        TypeKind.NONE -> JeNoneType
        else -> illegalArg("Must be kind of VOID or NONE, got ${kind.name}")
    }

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
            i, t -> (t as? JePsiType)?.psiType ?: throw IllegalArgumentException("Invalid type argument #$i: $t") 
        }

        val psiType = createDeclaredType(psiClass, typeArgs) ?: 
                      throw IllegalStateException("Can't create declared type ($psiClass, $typeArgs)")
        return JeDeclaredType(psiType, psiClass)
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
        
        val containingType = (containing as? JePsiType)?.psiType as? PsiClassType 
                ?: throw IllegalArgumentException("Illegal containing type: $containing")
        val containingClass = containingType.resolve() 
                ?: throw IllegalArgumentException("Class type can't be resolved: $containing")
        
        if (psiClass.containingClass != containingClass) {
            throw IllegalArgumentException("$containing is not an enclosing element for $typeElem")
        }

        val typeArgs = typeArgMirrors.mapIndexed {
            i, t -> (t as? JePsiType)?.psiType ?: throw IllegalArgumentException("Invalid type argument #$i: $t")
        }

        val psiType = createDeclaredType(psiClass, typeArgs) ?:
                      throw IllegalStateException("Can't create declared type ($psiClass, $typeArgs)")
        return JeDeclaredType(psiType, psiClass, containing)
    }

    private fun Array<out PsiType>.findSuperType(superTypeClass: PsiClass): PsiClassType? {
        for (supertype in this) {
            if (supertype is PsiClassType && supertype.resolve() == superTypeClass) return supertype
            supertype.superTypes.findSuperType(superTypeClass)?.let { return it }
        }
        return null
    }

    override fun asMemberOf(containing: DeclaredType, element: Element): TypeMirror {
        if (containing !is JeDeclaredType || element is JeClassInitializerExecutableElement) return element.asType()
        val containingType = containing.psiType

        val member = (element as JeElement).psi as? PsiMember ?: return element.asType()
        val methodContainingClass = member.containingClass ?: return element.asType()

        val relevantSuperType = containingType.superTypes.findSuperType(methodContainingClass) ?: return element.asType()
        val resolveResult = relevantSuperType.resolveGenerics()
        if (!resolveResult.isValidResult) return element.asType()
        val substitutor = resolveResult.substitutor

        return when (element) {
            is JeMethodExecutableElement -> {
                val method = element.psi
                val signature = method.getSignature(substitutor)
                val returnType = substitutor.substitute(element.psi.returnType)
                JeMethodExecutableTypeMirror(method, signature, returnType)
            }
            is JeVariableElement -> substitutor.substitute(element.psi.type).toJeType(psiManager)
            else -> throw IllegalArgumentException("Invalid element type: $element")
        }
    }

    override fun isSubsignature(t1: ExecutableType, t2: ExecutableType): Boolean {
        val m1 = (t1 as JeClassInitializerExecutableTypeMirror).initializer
        val m2 = (t2 as JeClassInitializerExecutableTypeMirror).initializer
        
        // No parameters
        if (m1 is PsiClassInitializer && m2 is PsiClassInitializer) return true
        
        if (m1 !is PsiMethod || m2 !is PsiMethod) return false
        
        if (m1.parameterList.parametersCount != m2.parameterList.parametersCount) return false
        
        for (i in 0..(m1.parameterList.parametersCount - 1)) {
            val p1 = m1.parameterList.parameters[i].type
            val p2 = m2.parameterList.parameters[i].type
            
            if (p1 != p2 && TypeConversionUtil.erasure(p1) != TypeConversionUtil.erasure(p2)) return false
        }
        
        return true
    }

    override fun capture(t: TypeMirror): TypeMirror? {
        TODO()
    }
}

private fun illegalArg(text: String? = null): Nothing {
    val message = if (text != null) ": $text" else ""
    throw IllegalArgumentException("Illegal argument" + message)
}

private fun assertKindNot(typeMirror: TypeMirror, vararg kinds: TypeKind): Unit {
    if (typeMirror.kind in kinds) illegalArg("type must not be kind of " + kinds.joinToString { it.name } + ", got ${typeMirror.kind.name}")
}

private fun assertJeType(type: TypeMirror) {
    if (type !is JeTypeMirror) {
        illegalArg("Must be a subclass of JePsiType, got ${type.javaClass.name}")
    }
}