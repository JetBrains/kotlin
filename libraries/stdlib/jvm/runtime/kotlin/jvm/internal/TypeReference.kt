/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.*

@SinceKotlin("1.4")
@Suppress("NEWER_VERSION_IN_SINCE_KOTLIN", "API_NOT_AVAILABLE" /* See KT-30129 */) // TODO: remove this in 1.4
public class TypeReference(
    override val classifier: KClassifier,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean
) : KType {
    override val annotations: List<Annotation>
        get() = emptyList()

    override fun equals(other: Any?): Boolean =
        other is TypeReference &&
                classifier == other.classifier && arguments == other.arguments && isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int =
        (classifier.hashCode() * 31 + arguments.hashCode()) * 31 + isMarkedNullable.hashCode()

    override fun toString(): String =
        asString() + Reflection.REFLECTION_NOT_AVAILABLE

    private fun asString(): String {
        val javaClass = (classifier as? KClass<*>)?.java
        val klass = when {
            javaClass == null -> classifier.toString()
            javaClass.isArray -> javaClass.arrayClassName
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

    // TODO: this should be the implementation of KTypeProjection.toString, see KT-30071
    @Suppress("NO_REFLECTION_IN_CLASS_PATH")
    private fun KTypeProjection.asString(): String {
        if (variance == null) return "*"

        val typeString = (type as? TypeReference)?.asString() ?: type.toString()
        return when (variance) {
            KVariance.INVARIANT -> typeString
            KVariance.IN -> "in $typeString"
            KVariance.OUT -> "out $typeString"
        }
    }
}
