/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.*

internal class KTypeParameterImpl(
        override val name: String,
        private val containerFqName: String,
        override val upperBounds: List<KType>,
        val varianceId: Int, // mapping is used to make static initialization possible
        override val isReified: Boolean
) : KTypeParameter {
    override val variance: KVariance
        get() = KVarianceMapper.varianceById(varianceId)!!

    constructor(name: String, containerFqName: String, upperBounds: List<KType>, variance: KVariance, isReified: Boolean) :
            this(name, containerFqName, upperBounds, KVarianceMapper.idByVariance(variance), isReified)

    override fun toString(): String = when (variance) {
        KVariance.INVARIANT -> ""
        KVariance.IN -> "in "
        KVariance.OUT -> "out "
    } + name

    override fun equals(other: Any?) =
            other is KTypeParameterImpl && name == other.name && containerFqName == other.containerFqName

    override fun hashCode() = containerFqName.hashCode() * 31 + name.hashCode()
}