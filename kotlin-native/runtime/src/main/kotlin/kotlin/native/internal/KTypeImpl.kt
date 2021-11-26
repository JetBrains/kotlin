/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.*

internal object KVarianceMapper {
    // this constants are copypasted to ReflectionSupport.kt
    const val VARIANCE_STAR = -1
    const val VARIANCE_INVARIANT = 0
    const val VARIANCE_IN = 1
    const val VARIANCE_OUT = 2

    fun idByVariance(variance: KVariance) = when (variance) {
        KVariance.INVARIANT -> VARIANCE_INVARIANT
        KVariance.IN -> VARIANCE_IN
        KVariance.OUT -> VARIANCE_OUT
    }

    fun varianceById(id: Int) = when (id) {
        VARIANCE_STAR -> null
        VARIANCE_INVARIANT -> KVariance.INVARIANT
        VARIANCE_IN -> KVariance.IN
        VARIANCE_OUT -> KVariance.OUT
        else -> throw IllegalStateException("Unknown variance id ${id}")
    }
}

/*
 * This class is used to avoid having enum inside KType class
 * Static initialization for enum objects is not supported yet,
 * so to initialize KType statically we need to avoid them.
 *
 * When this issue is resolved, this class can be replaced with just ArrayList
 */
internal class KTypeProjectionList(val variance: IntArray, val type: Array<KType?>) : AbstractList<KTypeProjection>() {
    override val size
        get() = variance.size


    override fun get(index: Int) : KTypeProjection {
        AbstractList.checkElementIndex(index, size)
        val kVariance = KVarianceMapper.varianceById(variance[index]) ?: return KTypeProjection.STAR
        return KTypeProjection(kVariance, type[index])
    }

}

internal class KTypeImpl<T>(
        override val classifier: KClassifier?,
        override val arguments: List<KTypeProjection>,
        override val isMarkedNullable: Boolean
) : KType {

    @ExportForCompiler
    @ConstantConstructorIntrinsic("KTYPE_IMPL")
    @Suppress("UNREACHABLE_CODE")
    constructor() : this(null, TODO("This is intrinsic constructor and it shouldn't be used directly"), false)

    override fun equals(other: Any?) =
            other is KTypeImpl<*> &&
                    this.classifier == other.classifier &&
                    this.arguments == other.arguments &&
                    this.isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int {
        return (classifier?.hashCode() ?: 0) * 31 * 31 + this.arguments.hashCode() * 31 + if (isMarkedNullable) 1 else 0
    }

    override fun toString(): String {
        val classifierString = when (classifier) {
            is KClass<*> -> classifier.qualifiedName ?: classifier.simpleName
            is KTypeParameter -> classifier.name
            else -> null
        } ?: return "(non-denotable type)"

        return buildString {
            append(classifierString)

            if (arguments.isNotEmpty()) {
                append('<')

                arguments.forEachIndexed { index, argument ->
                    if (index > 0) append(", ")

                    append(argument)
                }

                append('>')
            }

            if (isMarkedNullable) append('?')
        }
    }
}

internal class KTypeImplForTypeParametersWithRecursiveBounds : KType {
    override val classifier: KClassifier?
        get() = error("Type parameters with recursive bounds are not yet supported in reflection")

    override val arguments: List<KTypeProjection> get() = emptyList()

    override val isMarkedNullable: Boolean
        get() = error("Type parameters with recursive bounds are not yet supported in reflection")

    override fun equals(other: Any?) =
            error("Type parameters with recursive bounds are not yet supported in reflection")

    override fun hashCode(): Int =
            error("Type parameters with recursive bounds are not yet supported in reflection")
}
