/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.*

internal class KTypeParameterImpl(
        override val name: String,
        override val containerFqName: String,
        private val upperBoundsArray: Array<KType>,
        val varianceId: Int, // mapping is used to make static initialization possible
        override val isReified: Boolean
) : KTypeParameterBase() {
    override val upperBounds: List<KType>
        get() = upperBoundsArray.asList()
    override val variance: KVariance
        get() = KVarianceMapper.varianceById(varianceId)!!
}
