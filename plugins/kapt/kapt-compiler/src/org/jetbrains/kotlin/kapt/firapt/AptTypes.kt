/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.Types

/** [Types] over the FIR-built model, following the JLS rules that annotation processors rely on. */
internal class AptTypes(private val env: AptEnv) : Types {
    private val boxes = mapOf(
        TypeKind.BOOLEAN to "java/lang/Boolean", TypeKind.BYTE to "java/lang/Byte", TypeKind.CHAR to "java/lang/Character",
        TypeKind.SHORT to "java/lang/Short", TypeKind.INT to "java/lang/Integer", TypeKind.LONG to "java/lang/Long",
        TypeKind.FLOAT to "java/lang/Float", TypeKind.DOUBLE to "java/lang/Double",
    )
    private val unboxes = boxes.entries.associate { it.value to it.key }

    override fun asElement(t: TypeMirror): Element? = when (t) {
        is AptDeclaredType -> t.element
        is AptTypeVariable -> t.element
        else -> null
    }

    override fun isSameType(t1: TypeMirror, t2: TypeMirror): Boolean {
        if (t1.kind == TypeKind.WILDCARD || t2.kind == TypeKind.WILDCARD) return false
        return sameType(t1, t2)
    }

    private fun sameType(t1: TypeMirror, t2: TypeMirror): Boolean = when {
        t1 is AptDeclaredType && t2 is AptDeclaredType -> t1.element === t2.element &&
                t1.typeArguments.size == t2.typeArguments.size &&
                t1.typeArguments.zip(t2.typeArguments).all { sameTypeArgument(it.first, it.second) }
        t1 is AptArrayType && t2 is AptArrayType -> sameType(t1.componentType, t2.componentType)
        t1 is AptIntersectionType && t2 is AptIntersectionType -> t1.bounds.size == t2.bounds.size &&
                t1.bounds.zip(t2.bounds).all { sameType(it.first, it.second) }
        else -> t1 == t2
    }

    private fun sameTypeArgument(a: TypeMirror, b: TypeMirror): Boolean {
        if (a is WildcardType && b is WildcardType) {
            return nullableSame(a.extendsBound, b.extendsBound) && nullableSame(a.superBound, b.superBound)
        }
        return sameType(a, b)
    }

    private fun nullableSame(a: TypeMirror?, b: TypeMirror?) = if (a == null || b == null) a == b else sameType(a, b)

    override fun isSubtype(t1: TypeMirror, t2: TypeMirror): Boolean {
        if (t1.kind == TypeKind.ERROR || t2.kind == TypeKind.ERROR) return true
        if (t1.kind.isPrimitive || t2.kind.isPrimitive) return t1.kind.isPrimitive && t2.kind.isPrimitive && primitiveWidens(t1.kind, t2.kind)
        if (t2 is AptIntersectionType) return t2.bounds.all { isSubtype(t1, it) }
        return when (t1) {
            is NullType -> t2.kind == TypeKind.DECLARED || t2.kind == TypeKind.ARRAY || t2.kind == TypeKind.TYPEVAR || t2 is NullType
            is AptTypeVariable -> sameType(t1, t2) || (t2 !is AptTypeVariable || t2.lowerBound !is NullType) && isSubtype(t1.upperBound, t2)
            is AptIntersectionType -> t1.bounds.any { isSubtype(it, t2) }
            is AptArrayType -> when (t2) {
                is AptArrayType -> if (t1.componentType.kind.isPrimitive) t1.componentType.kind == t2.componentType.kind
                else isSubtype(t1.componentType, t2.componentType)
                is AptDeclaredType -> t2.element.internalName in ARRAY_SUPERTYPES
                else -> false
            }
            is AptDeclaredType -> when (t2) {
                is AptDeclaredType -> {
                    val sup = asSuper(t1, t2.element) ?: return false
                    if (t2.typeArguments.isEmpty()) return true
                    if (sup.typeArguments.isEmpty()) return false
                    sup.typeArguments.size == t2.typeArguments.size &&
                            t2.typeArguments.zip(sup.typeArguments).all { containsType(it.first, it.second) }
                }
                else -> false
            }
            else -> sameType(t1, t2)
        }
    }

    private fun primitiveWidens(from: TypeKind, to: TypeKind): Boolean {
        if (from == to) return true
        val order = listOf(TypeKind.BYTE, TypeKind.SHORT, TypeKind.INT, TypeKind.LONG, TypeKind.FLOAT, TypeKind.DOUBLE)
        if (from == TypeKind.CHAR) return to in order.drop(2)
        if (from == TypeKind.BOOLEAN || to == TypeKind.BOOLEAN || to == TypeKind.CHAR) return false
        return order.indexOf(from) < order.indexOf(to)
    }

    private fun containsType(s: TypeMirror, t: TypeMirror): Boolean {
        if (s !is WildcardType) return t !is WildcardType && sameType(s, t)
        val upper = s.extendsBound
        val lower = s.superBound
        return when {
            upper != null -> when {
                t is WildcardType -> t.extendsBound?.let { isSubtype(it, upper) } ?: isSameType(upper, env.objectType)
                else -> isSubtype(t, upper)
            }
            lower != null -> when {
                t is WildcardType -> t.superBound?.let { isSubtype(lower, it) } ?: false
                else -> isSubtype(lower, t)
            }
            else -> true
        }
    }

    override fun isAssignable(t1: TypeMirror, t2: TypeMirror): Boolean {
        if (isSubtype(t1, t2)) return true
        if (t1.kind.isPrimitive && t2.kind == TypeKind.DECLARED) return isSubtype(boxedClass(t1 as PrimitiveType).asType(), t2)
        if (t1 is AptDeclaredType && t2.kind.isPrimitive) {
            val unboxed = unboxes[t1.element.internalName] ?: return false
            return primitiveWidens(unboxed, t2.kind)
        }
        // Unchecked conversion of a raw type.
        if (t1 is AptDeclaredType && t2 is AptDeclaredType && t1.typeArguments.isEmpty()) return asSuper(t1, t2.element) != null
        return false
    }

    override fun contains(t1: TypeMirror, t2: TypeMirror): Boolean = containsType(t1, t2)

    override fun isSubsignature(m1: ExecutableType, m2: ExecutableType): Boolean {
        val p1 = m1.parameterTypes
        val p2 = m2.parameterTypes
        if (p1.size != p2.size) return false
        if (p1.zip(p2).all { isSameType(it.first, it.second) }) return true
        return p1.zip(p2).all { isSameType(it.first, erasure(it.second)) }
    }

    override fun directSupertypes(t: TypeMirror): List<TypeMirror> = when (t) {
        is AptDeclaredType -> directSupertypesOf(t)
        is AptTypeVariable -> listOf(t.upperBound)
        else -> emptyList()
    }

    private fun directSupertypesOf(t: AptDeclaredType): List<TypeMirror> {
        val element = t.element
        val substitution = substitutionOf(t)
        val raw = t.typeArguments.isEmpty() && element.typeParameters.isNotEmpty()
        fun map(type: TypeMirror) = if (raw) erasure(type) else substitute(type, substitution)
        val result = mutableListOf<TypeMirror>()
        val superclass = element.superclass
        if (superclass.kind == TypeKind.DECLARED || superclass.kind == TypeKind.ERROR) result += map(superclass)
        else if (element.kind == ElementKind.INTERFACE || element.kind == ElementKind.ANNOTATION_TYPE) result += env.objectType
        element.interfaces.mapTo(result) { map(it) }
        return result
    }

    /** The supertype of [t] whose element is [target], with the type arguments substituted, or `null` if there is none. */
    fun asSuper(t: AptDeclaredType, target: TypeElement): AptDeclaredType? {
        if (t.element === target) return t
        if ((target as? AptTypeElement)?.internalName == "java/lang/Object") return target.rawType
        for (supertype in directSupertypesOf(t)) {
            if (supertype is AptDeclaredType && supertype !is AptErrorType) asSuper(supertype, target)?.let { return it }
        }
        return null
    }

    private fun substitutionOf(t: AptDeclaredType): Map<AptTypeParameterElement, TypeMirror> {
        val result = HashMap<AptTypeParameterElement, TypeMirror>()
        var current: TypeMirror = t
        while (current is AptDeclaredType) {
            current.element.typeParameters.zip(current.typeArguments).forEach { result[it.first] = it.second }
            current = current.enclosingType
        }
        return result
    }

    fun substitute(type: TypeMirror, substitution: Map<AptTypeParameterElement, TypeMirror>): TypeMirror {
        if (substitution.isEmpty()) return type
        return when (type) {
            is AptTypeVariable -> substitution[type.element] ?: type
            is AptErrorType -> type
            is AptDeclaredType -> {
                if (type.typeArguments.isEmpty() && type.enclosingType !is AptDeclaredType) return type
                AptDeclaredType(type.element, type.typeArguments.map { substitute(it, substitution) }, substitute(type.enclosingType, substitution))
            }
            is AptArrayType -> AptArrayType(substitute(type.componentType, substitution))
            is AptWildcardType -> AptWildcardType(type.extendsBound?.let { substitute(it, substitution) }, type.superBound?.let { substitute(it, substitution) })
            is AptIntersectionType -> AptIntersectionType(type.bounds.map { substitute(it, substitution) })
            is AptExecutableType -> AptExecutableType(
                type.typeVariables, substitute(type.returnType, substitution), type.parameterTypes.map { substitute(it, substitution) },
                type.thrownTypes.map { substitute(it, substitution) },
            )
            else -> type
        }
    }

    override fun erasure(t: TypeMirror): TypeMirror = when (t) {
        is AptErrorType -> t
        is AptDeclaredType -> t.element.rawType
        is AptTypeVariable -> erasure(t.element.boundTypes.firstOrNull() ?: env.objectType)
        is AptArrayType -> AptArrayType(erasure(t.componentType))
        is AptIntersectionType -> erasure(t.bounds.first())
        is AptWildcardType -> erasure(t.extendsBound ?: env.objectType)
        is AptExecutableType -> AptExecutableType(emptyList(), erasure(t.returnType), t.parameterTypes.map { erasure(it) }, t.thrownTypes.map { erasure(it) })
        else -> t
    }

    override fun boxedClass(p: PrimitiveType): TypeElement = env.typeElementOrError(boxes.getValue(p.kind))

    override fun unboxedType(t: TypeMirror): PrimitiveType {
        val kind = (t as? AptDeclaredType)?.let { unboxes[it.element.internalName] }
            ?: throw IllegalArgumentException("This type cannot be unboxed: $t")
        return env.primitive(kind)
    }

    override fun capture(t: TypeMirror): TypeMirror = t

    override fun getPrimitiveType(kind: TypeKind): PrimitiveType {
        require(kind.isPrimitive) { "Not a primitive type: $kind" }
        return env.primitive(kind)
    }

    override fun getNullType(): NullType = AptNullType

    override fun getNoType(kind: TypeKind): NoType = when (kind) {
        TypeKind.VOID -> env.voidType
        TypeKind.NONE -> env.noneType
        else -> throw IllegalArgumentException(kind.toString())
    }

    override fun getArrayType(componentType: TypeMirror): ArrayType = AptArrayType(componentType)

    override fun getWildcardType(extendsBound: TypeMirror?, superBound: TypeMirror?): WildcardType = AptWildcardType(extendsBound, superBound)

    override fun getDeclaredType(typeElem: TypeElement, vararg typeArgs: TypeMirror): DeclaredType {
        val element = typeElem as AptTypeElement
        if (typeArgs.isEmpty()) return element.rawType
        require(typeArgs.size == element.typeParameters.size) { "Wrong number of type arguments for $typeElem" }
        return AptDeclaredType(element, typeArgs.toList(), element.defaultEnclosingType)
    }

    override fun getDeclaredType(containing: DeclaredType?, typeElem: TypeElement, vararg typeArgs: TypeMirror): DeclaredType {
        if (containing == null) return getDeclaredType(typeElem, *typeArgs)
        val element = typeElem as AptTypeElement
        return AptDeclaredType(element, typeArgs.toList(), containing)
    }

    override fun asMemberOf(containing: DeclaredType, element: Element): TypeMirror {
        val owner = generateSequence(element.enclosingElement) { it.enclosingElement }.firstOrNull { it is TypeElement } as? AptTypeElement
            ?: return element.asType()
        val sup = asSuper(containing as AptDeclaredType, owner) ?: return element.asType()
        if (sup.typeArguments.isEmpty() && owner.typeParameters.isNotEmpty()) return erasure(element.asType())
        return substitute(element.asType(), substitutionOf(sup))
    }

    private companion object {
        val ARRAY_SUPERTYPES = setOf("java/lang/Object", "java/lang/Cloneable", "java/io/Serializable")
    }
}
