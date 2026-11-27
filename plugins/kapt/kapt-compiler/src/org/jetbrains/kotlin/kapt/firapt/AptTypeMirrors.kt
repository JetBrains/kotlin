/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.type.*

internal abstract class AptTypeMirror : TypeMirror {
    override fun getAnnotationMirrors(): List<AnnotationMirror> = emptyList()
    override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? = null

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<A> =
        java.lang.reflect.Array.newInstance(annotationType, 0) as Array<A>
}

internal class AptPrimitiveType(private val kind: TypeKind) : AptTypeMirror(), PrimitiveType {
    override fun getKind() = kind
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitPrimitive(this, p)
    override fun equals(other: Any?) = other is AptPrimitiveType && other.kind == kind
    override fun hashCode() = kind.hashCode()
    override fun toString() = kind.name.lowercase()
}

internal class AptNoType(private val kind: TypeKind) : AptTypeMirror(), NoType {
    override fun getKind() = kind
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitNoType(this, p)
    override fun equals(other: Any?) = other is AptNoType && other.kind == kind
    override fun hashCode() = kind.hashCode()
    override fun toString() = when (kind) {
        TypeKind.VOID -> "void"
        TypeKind.PACKAGE -> "package"
        else -> "none"
    }
}

internal object AptNullType : AptTypeMirror(), NullType {
    override fun getKind() = TypeKind.NULL
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitNull(this, p)
    override fun toString() = "<nulltype>"
}

internal class AptArrayType(private val component: TypeMirror) : AptTypeMirror(), ArrayType {
    override fun getKind() = TypeKind.ARRAY
    override fun getComponentType() = component
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitArray(this, p)
    override fun equals(other: Any?) = other is AptArrayType && other.component == component
    override fun hashCode() = component.hashCode() * 31 + 1
    override fun toString() = "$component[]"
}

internal open class AptDeclaredType(
    val element: AptTypeElement,
    private val typeArguments: List<TypeMirror>,
    private val enclosing: TypeMirror,
) : AptTypeMirror(), DeclaredType {
    override fun getKind() = TypeKind.DECLARED
    override fun asElement(): AptTypeElement = element
    override fun getEnclosingType(): TypeMirror = enclosing
    override fun getTypeArguments(): List<TypeMirror> = typeArguments
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitDeclared(this, p)

    val isRaw: Boolean get() = typeArguments.isEmpty() && element.typeParameters.isNotEmpty()

    override fun equals(other: Any?) = other is AptDeclaredType && other !is AptErrorType &&
            other.element === element && other.typeArguments == typeArguments && other.enclosing == enclosing

    override fun hashCode() = System.identityHashCode(element) * 31 + typeArguments.hashCode()

    override fun toString(): String = buildString {
        if (enclosing is AptDeclaredType && enclosing.typeArguments.isNotEmpty()) {
            append(enclosing).append('.').append(element.simpleName)
        } else {
            append(element.qualifiedName)
        }
        if (typeArguments.isNotEmpty()) typeArguments.joinTo(this, ",", "<", ">")
    }
}

/** A class that is not found. [element] is a stand-in that has no members. */
internal class AptErrorType(element: AptTypeElement) : AptDeclaredType(element, emptyList(), AptNoType(TypeKind.NONE)), ErrorType {
    override fun getKind() = TypeKind.ERROR
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitError(this, p)
    override fun equals(other: Any?) = other is AptErrorType && other.element === element
    override fun hashCode() = System.identityHashCode(element)
    override fun toString() = element.qualifiedName.toString()
}

internal class AptTypeVariable(val element: AptTypeParameterElement) : AptTypeMirror(), TypeVariable {
    override fun getKind() = TypeKind.TYPEVAR
    override fun asElement(): Element = element

    override fun getUpperBound(): TypeMirror {
        val bounds = element.boundTypes
        return when (bounds.size) {
            0 -> element.env.objectType
            1 -> bounds.single()
            else -> AptIntersectionType(bounds)
        }
    }

    override fun getLowerBound(): TypeMirror = AptNullType
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitTypeVariable(this, p)
    override fun equals(other: Any?) = other is AptTypeVariable && other.element === element
    override fun hashCode() = System.identityHashCode(element)
    override fun toString() = element.simpleName.toString()
}

internal class AptWildcardType(private val extendsBound: TypeMirror?, private val superBound: TypeMirror?) : AptTypeMirror(), WildcardType {
    override fun getKind() = TypeKind.WILDCARD
    override fun getExtendsBound() = extendsBound
    override fun getSuperBound() = superBound
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitWildcard(this, p)
    override fun equals(other: Any?) = other is AptWildcardType && other.extendsBound == extendsBound && other.superBound == superBound
    override fun hashCode() = (extendsBound?.hashCode() ?: 0) * 31 + (superBound?.hashCode() ?: 0) + 7
    override fun toString() = when {
        extendsBound != null -> "? extends $extendsBound"
        superBound != null -> "? super $superBound"
        else -> "?"
    }
}

internal class AptIntersectionType(private val bounds: List<TypeMirror>) : AptTypeMirror(), IntersectionType {
    override fun getKind() = TypeKind.INTERSECTION
    override fun getBounds() = bounds
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitIntersection(this, p)
    override fun equals(other: Any?) = other is AptIntersectionType && other.bounds == bounds
    override fun hashCode() = bounds.hashCode()
    override fun toString() = bounds.joinToString("&")
}

internal class AptExecutableType(
    private val typeVariables: List<TypeVariable>,
    private val returnType: TypeMirror,
    private val parameterTypes: List<TypeMirror>,
    private val thrownTypes: List<TypeMirror>,
) : AptTypeMirror(), ExecutableType {
    override fun getKind() = TypeKind.EXECUTABLE
    override fun getTypeVariables() = typeVariables
    override fun getReturnType() = returnType
    override fun getParameterTypes() = parameterTypes
    override fun getReceiverType(): TypeMirror = AptNoType(TypeKind.NONE)
    override fun getThrownTypes() = thrownTypes
    override fun <R, P> accept(v: TypeVisitor<R, P>, p: P): R = v.visitExecutable(this, p)

    override fun equals(other: Any?) = other is AptExecutableType && other.typeVariables == typeVariables &&
            other.returnType == returnType && other.parameterTypes == parameterTypes && other.thrownTypes == thrownTypes

    override fun hashCode() = parameterTypes.hashCode() * 31 + returnType.hashCode()

    override fun toString() = buildString {
        if (typeVariables.isNotEmpty()) typeVariables.joinTo(this, ",", "<", ">")
        parameterTypes.joinTo(this, ",", "(", ")")
        append(returnType)
    }
}

