/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")

package kotlin.jvm.internal

import kotlin.reflect.*

@SinceKotlin("1.4")
public class TypeReference /* @SinceKotlin("1.6") constructor */(
    override val classifier: KClassifier,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean,
    internal val platformTypeUpperBound: KType?,
    internal val mutableCollectionType: Boolean,
) : KType {
    constructor(
        classifier: KClassifier,
        arguments: List<KTypeProjection>,
        isMarkedNullable: Boolean,
    ) : this(classifier, arguments, isMarkedNullable, null, false)

    override val annotations: List<Annotation>
        get() = emptyList()

    override fun equals(other: Any?): Boolean =
        other is TypeReference &&
                classifier == other.classifier && arguments == other.arguments && isMarkedNullable == other.isMarkedNullable &&
                platformTypeUpperBound == other.platformTypeUpperBound && mutableCollectionType == other.mutableCollectionType

    override fun hashCode(): Int =
        (classifier.hashCode() * 31 + arguments.hashCode()) * 31 + isMarkedNullable.hashCode()

    override fun toString(): String =
        asString(false) + Reflection.REFLECTION_NOT_AVAILABLE

    private fun asString(convertPrimitiveToWrapper: Boolean): String {
        val javaClass = (classifier as? KClass<*>)?.java
        val klass = when {
            javaClass == null -> classifier.toString()
            javaClass.isArray -> javaClass.arrayClassName
            convertPrimitiveToWrapper && javaClass.isPrimitive -> (classifier as KClass<*>).javaObjectType.name
            else -> javaClass.name
        }
        val args =
            if (arguments.isEmpty()) ""
            else arguments.joinToString(", ", "<", ">") { it.asString() }
        val nullable = if (isMarkedNullable) "?" else ""

        return klass + args + nullable
    }

    private val Class<*>.arrayClassName
        get() = when (this) {
            BooleanArray::class.java -> "kotlin.BooleanArray"
            CharArray::class.java -> "kotlin.CharArray"
            ByteArray::class.java -> "kotlin.ByteArray"
            ShortArray::class.java -> "kotlin.ShortArray"
            IntArray::class.java -> "kotlin.IntArray"
            FloatArray::class.java -> "kotlin.FloatArray"
            LongArray::class.java -> "kotlin.LongArray"
            DoubleArray::class.java -> "kotlin.DoubleArray"
            else -> "kotlin.Array"
        }

    // This is based on KTypeProjection.toString, but uses [asString] to avoid adding the
    // "reflection not supported" suffix to each type argument.
    private fun KTypeProjection.asString(): String {
        if (variance == null) return "*"

        val typeString = (type as? TypeReference)?.asString(true) ?: type.toString()
        return when (variance) {
            KVariance.INVARIANT -> typeString
            KVariance.IN -> "in $typeString"
            KVariance.OUT -> "out $typeString"
        }
    }
}
