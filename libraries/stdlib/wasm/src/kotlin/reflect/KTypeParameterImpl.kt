/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.*

internal data class KTypeParameterImpl(
    override val name: String,
    override val upperBounds: List<KType>,
    override val variance: KVariance,
    override val isReified: Boolean,
    private val container: String,
) : KTypeParameter {
    override fun toString(): String = when (variance) {
        KVariance.INVARIANT -> ""
        KVariance.IN -> "in "
        KVariance.OUT -> "out "
    } + name
}