/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

class SirTypeParameter(
    val name: String,
    val variance: SirTypeVariance = SirTypeVariance.INVARIANT,
    val bounds: MutableList<SirType> = mutableListOf(),
    val isPrimaryAssociatedType: Boolean = false,
    val origin: SirOrigin = SirOrigin.Unknown,
) {
    constructor(
        name: String,
        bounds: List<SirType>,
        variance: SirTypeVariance = SirTypeVariance.INVARIANT,
        isPrimaryAssociatedType: Boolean = false,
        origin: SirOrigin = SirOrigin.Unknown,
    ) : this(name, variance, bounds.toMutableList(), isPrimaryAssociatedType, origin)

    override fun toString(): String = "SirTypeParameter($name)"
}
