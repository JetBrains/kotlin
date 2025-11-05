/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

import java.lang.reflect.*
import kotlin.internal.LowPriorityInOverloadResolution
import kotlin.jvm.internal.KTypeBase
import kotlin.jvm.internal.KTypeParameterBase

/**
 * Returns a Java [Type] instance corresponding to the given Kotlin type.
 *
 * This function is experimental because not all the features are supported yet, and behavior might change in corner cases.
 * In particular, the following is not supported correctly or at all:
 * - declaration-site variance
 * - variance of types annotated with [JvmSuppressWildcards]
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@LowPriorityInOverloadResolution // To make non-experimental kotlin.reflect.full.javaType always win in overload resolution
public val KType.javaType: Type
    get() {
        if (this is KTypeBase) {
            // Use kotlin-reflect implementation for types which are position-dependent, e.g. "Unit" in a callable's return type.
            javaType?.let { return it }
        }

        return computeJavaType()
    }

@ExperimentalStdlibApi
private fun KType.computeJavaType(forceWrapper: Boolean = false): Type {
    when (val classifier = classifier) {
        is KTypeParameter -> {
            val container = (classifier as? KTypeParameterBase)?.javaContainingDeclaration
                ?: throw UnsupportedOperationException("javaType is not supported for this type: $this")
            return container.typeParameters.single { it.name == classifier.name }
        }
        is KClass<*> -> {
            val jClass = if (forceWrapper) classifier.javaObjectType else classifier.java
            val arguments = arguments
            if (arguments.isEmpty()) return jClass

            if (jClass.isArray) {
                if (jClass.componentType.isPrimitive) return jClass

                val (variance, elementType) = arguments.singleOrNull()
                    ?: throw IllegalArgumentException("kotlin.Array must have exactly one type argument: $this")
                return when (variance) {
                    // Array<in ...> is always erased to Object[], and Array<*> is Object[].
                    null, KVariance.IN -> jClass
                    KVariance.INVARIANT, KVariance.OUT -> {
                        val javaElementType = elementType!!.computeJavaType()
                        if (javaElementType is Class<*>) jClass else GenericArrayTypeImpl(javaElementType)
                    }
                }
            }

            return createPossiblyInnerType(jClass, arguments)
        }
        else -> throw UnsupportedOperationException("Unsupported type classifier: $this")
    }
}

@ExperimentalStdlibApi
private fun createPossiblyInnerType(jClass: Class<*>, arguments: List<KTypeProjection>): Type {
    val ownerClass = jClass.declaringClass
        ?: return ParameterizedTypeImpl(jClass, null, arguments.map(KTypeProjection::javaType))
    if (Modifier.isStatic(jClass.modifiers))
        return ParameterizedTypeImpl(jClass, ownerClass, arguments.map(KTypeProjection::javaType))

    val n = jClass.typeParameters.size
    return ParameterizedTypeImpl(
        jClass,
        createPossiblyInnerType(ownerClass, arguments.subList(n, arguments.size)),
        arguments.subList(0, n).map(KTypeProjection::javaType)
    )
}

@ExperimentalStdlibApi
private val KTypeProjection.javaType: Type
    get() {
        val variance = variance ?: return WildcardTypeImpl.STAR
        val type = type!!
        // TODO: JvmSuppressWildcards
        return when (variance) {
            KVariance.INVARIANT -> {
                // TODO: declaration-site variance
                type.computeJavaType(forceWrapper = true)
            }
            KVariance.IN -> WildcardTypeImpl(null, type.computeJavaType(forceWrapper = true))
            KVariance.OUT -> WildcardTypeImpl(type.computeJavaType(forceWrapper = true), null)
        }
    }

@ExperimentalStdlibApi
private interface TypeImpl : Type {
    // This is a copy of [Type.getTypeName] which is present on JDK 8+.
    @Suppress(
        "VIRTUAL_MEMBER_HIDDEN"
    ) // This is needed for cases when environment variable JDK_1_6 points to JDK 8+.
    fun getTypeName(): String
}

@ExperimentalStdlibApi
private class GenericArrayTypeImpl(private val elementType: Type) : GenericArrayType, TypeImpl {
    override fun getGenericComponentType(): Type = elementType

    override fun getTypeName(): String = "${typeToString(elementType)}[]"

    override fun equals(other: Any?): Boolean = other is GenericArrayType && genericComponentType == other.genericComponentType

    override fun hashCode(): Int = genericComponentType.hashCode()

    override fun toString(): String = getTypeName()
}

@ExperimentalStdlibApi
private class WildcardTypeImpl(private val upperBound: Type?, private val lowerBound: Type?) : WildcardType, TypeImpl {
    override fun getUpperBounds(): Array<Type> =
        arrayOf(upperBound ?: Any::class.java)

    override fun getLowerBounds(): Array<Type> =
        if (lowerBound == null) emptyArray() else arrayOf(lowerBound)

    override fun getTypeName(): String = when {
        lowerBound != null -> "? super ${typeToString(lowerBound)}"
        upperBound != null && upperBound != Any::class.java -> "? extends ${typeToString(upperBound)}"
        else -> "?"
    }

    override fun equals(other: Any?): Boolean =
        other is WildcardType && upperBounds.contentEquals(other.upperBounds) && lowerBounds.contentEquals(other.lowerBounds)

    override fun hashCode(): Int =
        upperBounds.contentHashCode() xor lowerBounds.contentHashCode()

    override fun toString(): String = getTypeName()

    companion object {
        val STAR = WildcardTypeImpl(null, null)
    }
}

@ExperimentalStdlibApi
private class ParameterizedTypeImpl(
    private val rawType: Class<*>,
    private val ownerType: Type?,
    typeArguments: List<Type>
) : ParameterizedType, TypeImpl {
    private val typeArguments = typeArguments.toTypedArray()

    override fun getRawType(): Type = rawType

    override fun getOwnerType(): Type? = ownerType

    override fun getActualTypeArguments(): Array<Type> = typeArguments

    override fun getTypeName(): String = buildString {
        if (ownerType != null) {
            append(typeToString(ownerType))
            append("$")
            append(rawType.simpleName)
        } else {
            append(typeToString(rawType))
        }

        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">", transform = ::typeToString)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ParameterizedType && rawType == other.rawType && ownerType == other.ownerType &&
                actualTypeArguments.contentEquals(other.actualTypeArguments)

    override fun hashCode(): Int =
        rawType.hashCode() xor ownerType.hashCode() xor actualTypeArguments.contentHashCode()

    override fun toString(): String = getTypeName()
}

// TODO: use getTypeName instead of this, when this code is migrated to JDK 8.
private fun typeToString(type: Type): String =
    if (type is Class<*>) {
        if (type.isArray) {
            val unwrap = generateSequence(type, Class<*>::getComponentType)
            unwrap.last().name + "[]".repeat(unwrap.count())
        } else type.name
    } else type.toString()
